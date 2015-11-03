/*
 * function : Logic to regulate the flow out of a master that is sending
 *            to multiple slaves -- we leverage VCs: we can send requests 
 *            with different return VCs and reorder back at the master
 * author   : Mohamed S. Abdelfattah
 * date     : 02-NOV-2015
 */

module tm_master_multislave_fast
#(
    parameter NUM_CREDITS = 32,
    parameter ADDRESS_WIDTH = 4,
    parameter VC_ADDRESS_WIDTH = 2,
	parameter WIDTH_DATA = 36
)
(
	input clk,
	input rst,
    
    //the sending bundle tells us when it's valid, and we tell it when we're ready to send
    input                               send_valid_in,
    output reg                          send_valid_out,
    
    input            [WIDTH_DATA-1 : 0] send_data_in,
    output reg       [WIDTH_DATA-1 : 0] send_data_out,
    
    input         [ADDRESS_WIDTH-1 : 0] send_dest_in,
    input         [ADDRESS_WIDTH-1 : 0] send_dest_out,
    
    input      [VC_ADDRESS_WIDTH-1 : 0] send_vc_in,
    input      [VC_ADDRESS_WIDTH-1 : 0] send_vc_out,
    
    output reg [VC_ADDRESS_WIDTH-1 : 0] send_ret_vc,  //this module decides the ret vc and attaches it to the packetizer
    
    input                               send_ready_in,  //coming from NoC
    output reg                          send_ready_out, // going to module
    
    
    output reg [VC_ADDRESS_WIDTH-1 : 0] curr_ret_vc,  //remove this and replace with ready signals (just for testing)
    
    //the receiving end tells us when a reply comes back, and we increment the credits
    //we also need to know if it's valid to be able to set the send ready correctly
    input [3:0] receive_valid
);


fifo_da
#(
    .WIDTH(VC_ADDRESS_WIDTH),
    .DEPTH(32) //just has to be bigger than the number of credits
)
fifo_inst
(
    .clk(clk),
    .clear(rst),
    .i_data_in(send_ret_vc),
    .i_write_en(send_ready_out),
    .i_full_out(),
    .o_data_out(curr_ret_vc),
    .o_read_en(|receive_valid),
    .o_empty_out()
);

localparam COUNTER_WIDTH = $clog2(NUM_CREDITS+1)+1;

//counter to keep track of the number of outstanding requests
//there is a separate counter for each return VC
reg [COUNTER_WIDTH-1:0] ret_vc_count [0:3];

//is the current VC currently being used?
reg [3:0] ret_vc_used;

//dest that is currently using this ret VC
reg [ADDRESS_WIDTH+VC_ADDRESS_WIDTH-1 : 0] ret_vc_slaveaddr [0:3];

reg ret_vc_assigned;

integer i;

//current dest/vc
reg [ADDRESS_WIDTH+VC_ADDRESS_WIDTH-1 : 0] sending_dst;
reg [ADDRESS_WIDTH+VC_ADDRESS_WIDTH-1 : 0] main_buffer_dst;
reg [ADDRESS_WIDTH+VC_ADDRESS_WIDTH-1 : 0] overflow_buffer_dst;

//skid buffer to store the data when we aren't allowed to send it
reg [WIDTH_DATA-1 : 0] main_buffer;
reg [WIDTH_DATA-1 : 0] overflow_buffer;
//indicates if we have something in the data buffer
reg main_buffer_valid;
reg overflow_buffer_valid;

reg predict_send;

//-------------------------------------------------------
// Implementation
//-------------------------------------------------------

//skid buffer to take incoming data
always @ (posedge clk)
begin
    if(rst)
    begin
        main_buffer <= 0;
        main_buffer_valid <= 0;
        overflow_buffer <= 0;
        overflow_buffer_valid <= 0;
        send_ready_out <= 0;
        send_data_out <= 0;
        send_valid_out <= 0;
        main_buffer_dst <= 0;
        overflow_buffer_dst <= 0;
        sending_dst <= 0;
        
        for(i=0; i < 4; i++)
        begin
            ret_vc_slaveaddr[i] <= 0;
        end
        ret_vc_used <= 0;
        ret_vc_assigned <= 0;
    end
    else
    begin
        //we're ready to receive
        if(~overflow_buffer_valid & ~main_buffer_valid)
        begin
            send_ready_out <= 1;
            if(send_valid_in)
            begin
                main_buffer <= send_data_in;
                main_buffer_valid <= 1;
                main_buffer_dst <= {send_vc_in,send_dest_in};
                send_ready_out <= 0; 
            end
        end
        
        //transfer data from overflow_buffer to main buffer and read incoming data
        else if (overflow_buffer_valid & ~main_buffer_valid)
        begin
            main_buffer <= overflow_buffer;
            main_buffer_valid <= 1;
            main_buffer_dst <= overflow_buffer_dst;
            
            overflow_buffer <= 0;
            overflow_buffer_valid <= 0;
            overflow_buffer_dst <= 0;
            send_ready_out <= 0;
            //if something is coming in, store in overflow
            if(send_valid_in)
            begin
                overflow_buffer <= send_data_in;
                overflow_buffer_valid <= 1;
                overflow_buffer_dst <= {send_vc_in,send_dest_in};
                send_ready_out <= 0;
            end
        end
        
        //read into main buffer
        else if (send_valid_in & ~main_buffer_valid)
        begin
            main_buffer <= send_data_in;
            main_buffer_valid <= 1;
            main_buffer_dst <= {send_vc_in,send_dest_in};
            send_ready_out <= 0;
        end
        
        //read into overflow buffer
        else if (send_valid_in & main_buffer_valid)
        begin
            //synposys translate off
            if(overflow_buffer_valid===1)
            begin
                $display("CAN'T ACCEPT MORE DATA!");
                $stop(1);
            end
            //synposys translate on
            overflow_buffer <= send_data_in;
            overflow_buffer_valid <= 1;
            overflow_buffer_dst <= {send_vc_in,send_dest_in};
            send_ready_out <= 0;
        end
        
        //------------------------------------------------
        // RET VC SELECTION
        //------------------------------------------------
        
        //this is a simple allocator
        //first free up any vcs that have no outstanding requests
        //are we already sending to this destination?
        //if we are, we'll append the same ret_vc
        //if not, we'll look for an unused vc and increment its counter
        //if no VCs are available, then we will stall
        for(i=0; i < 4; i++)
        begin
            //if there are no outstanding requests, set VC as available
            if(ret_vc_used[i] && ret_vc_count[i]==0 )
            begin
                ret_vc_used[i] = 0;
            end
            //if there are outstanding requests then we are using this vc
            if(~ret_vc_used[i] && ret_vc_count[i]>0)
            begin
                ret_vc_used[i] = 1;
            end
        end
        
        ret_vc_assigned = 0;
        for(i=0; i < 4; i++)
        begin
            //a ret VC is already being used for this slaveaddr
            if(ret_vc_assigned == 0 && ret_vc_used[i] && (main_buffer_dst==ret_vc_slaveaddr) )
            begin
                send_ret_vc = i;
                ret_vc_slaveaddr[i] = main_buffer_dst;
                ret_vc_assigned = 1;
            end
        end
        
        if(~ret_vc_assigned)
        begin
            for(i=0; i < 4; i++)
            begin
                //a ret VC is already being used for this slaveaddr
                if(ret_vc_assigned == 0 && ~ret_vc_used[i])
                begin
                    send_ret_vc = i;
                    ret_vc_slaveaddr[i] = main_buffer_dst;
                    ret_vc_used[i] = 1;
                    ret_vc_assigned = 1;
                end
            end
        end
        
        //------------------------------------------------
        // OUTPUT STAGE
        //------------------------------------------------
        
        //are we going to output data?
        //yes if we aren't switching slaves and have enough credits
        if(main_buffer_valid && ret_vc_assigned && ret_vc_count[send_ret_vc] < NUM_CREDITS && send_ready_in)
        begin
            send_data_out <= main_buffer;
            send_valid_out <= 1;
            main_buffer <= 0;
            main_buffer_valid <= 0;
            sending_dst <= main_buffer_dst;
            send_ready_out <= 1;
        end
        else 
        begin
            send_data_out <= 0;
            send_valid_out <= 0;
        end
        
    end //non reset
end

//keep track of the number of outstanding requests
always @ (posedge clk)
begin
	if (rst)
	begin
        for(i=0; i < 4; i++)
        begin
            ret_vc_count[i] <= 0;
        end
        predict_send = 0;
	end
	else
	begin
        
        predict_send = main_buffer_valid && ret_vc_assigned && ret_vc_count[send_ret_vc] < NUM_CREDITS && send_ready_in;
    
        if(predict_send===1'b1)
        begin
            //synopsys translate off
            if(ret_vc_count[send_ret_vc]  === NUM_CREDITS) begin
                $display("MULTISLAVE COUNTER VC %d OVERFLOW ERROR!",send_ret_vc);
                $finish(1);
            end
            //synopsys translate on
            ret_vc_count[send_ret_vc]  = ret_vc_count[send_ret_vc]  + 1;
        end
        
        for(i=0; i < 4; i++)
        begin
            if(receive_valid[i]===1'b1) 
            begin
                //synopsys translate off
                if(ret_vc_count[i]  === 0) begin
                    $display("MULTISLAVE COUNTER VC %d UNDERFLOW ERROR!",i);
                    $finish(1);
                end
                //synopsys translate on
                ret_vc_count[i]  = ret_vc_count[i]  - 1;
            end
        end
        
	end
end


endmodule




//-------------------------------------------------------------------------
// Helper modules
//-------------------------------------------------------------------------

/*
 * function : synchronous single-cycle fifo with "top" of queue always shown
 * author   : Mohamed S. Abdelfattah
 * date     : 26-AUG-2014
 * source   : asicworld.com
 */

module fifo_msf
#(
	parameter WIDTH = 4,
	parameter DEPTH = 4
)
(
	//clocks and reset
	input wire clk,
	input wire clear,
	
	//write port
	input  wire [WIDTH-1:0] i_data_in,
	input  wire             i_write_en,
	output reg              i_full_out,

	//read port
	output reg  [WIDTH-1:0] o_data_out,
	input  wire             o_read_en,
	output reg              o_empty_out
);

//address width
localparam ADDRESS_WIDTH = $clog2(DEPTH);

//storage
reg [WIDTH-1:0] memory [DEPTH-1:0];

//pointers to head/tail and enables
wire [ADDRESS_WIDTH-1:0] next_read_addr, next_write_addr;
wire                     next_read_en, next_write_en;

//misc
wire equal_address, set_status, rst_status, preset_full, preset_empty;
reg status;

//--------------------------------------------------------------------------------------
// Implementation
//--------------------------------------------------------------------------------------

//data in
always @ (posedge clk)
	if (i_write_en & ~i_full_out)
		memory[next_write_addr] <= i_data_in;

//data out
always @ (*)
	o_data_out <= memory[next_read_addr];


//read/write address enables
assign next_write_en  = i_write_en & ~i_full_out;
assign next_read_en = o_read_en & ~o_empty_out;

//read address
gray_counter_da
	#(.COUNTER_WIDTH(ADDRESS_WIDTH))
read_address_counter
(
	.gray_count_out(next_read_addr),
	.enable_in(next_read_en),
	.clear_in(clear),
	.clk(clk)
);

//write address
gray_counter_da
	#(.COUNTER_WIDTH(ADDRESS_WIDTH))
write_address_counter
(
	.gray_count_out(next_write_addr),
	.enable_in(next_write_en),
	.clear_in(clear),
	.clk(clk)
);

//equal address check
assign equal_address = (next_read_addr == next_write_addr);

//quadrant select
assign set_status = (next_write_addr[ADDRESS_WIDTH-2] ~^ next_read_addr[ADDRESS_WIDTH-1]) &
                    (next_write_addr[ADDRESS_WIDTH-1] ^  next_read_addr[ADDRESS_WIDTH-2]);

assign rst_status = (next_write_addr[ADDRESS_WIDTH-2] ^  next_read_addr[ADDRESS_WIDTH-1]) &
                    (next_write_addr[ADDRESS_WIDTH-1] ~^ next_read_addr[ADDRESS_WIDTH-2]);

//status logic: are we going full (status = 1) or going empty (status = 0)
always @ *
	if(rst_status | clear)
		status = 1'b0;
	else if (set_status)
		status = 1'b1;

//full_out logic
assign preset_full = status & equal_address;

always @ (posedge clk, posedge preset_full)
	if(preset_full)
		i_full_out <= 1'b1;
	else
		i_full_out <= 1'b0;

//empty_out logic 
assign preset_empty = ~status & equal_address;

always @ (posedge clk, posedge preset_empty)
	if(preset_empty)
		o_empty_out <= 1'b1;
	else
		o_empty_out <= 1'b0;

endmodule

/*
 * function : gray counter
 * author   : Mohamed S. Abdelfattah
 * date     : 26-AUG-2014
 * source   : asicworld.com
 */	

module gray_counter_da
#(parameter   COUNTER_WIDTH = 4) 
(
	output reg  [COUNTER_WIDTH-1:0]    gray_count_out,  //'Gray' code count output.
	
	input wire                         enable_in,  //Count enable.
	input wire                         clear_in,   //Count reset.
					    
	input wire                         clk
);

//Internal connections  variables
reg    [COUNTER_WIDTH-1:0]  binary_count;

//-------------------------------------------------------
// Implementation
//-------------------------------------------------------
				            
always @ (posedge clk or posedge clear_in)
	
	if (clear_in)
	begin
		binary_count <= {COUNTER_WIDTH{1'b0}} + 1; //gray count begins @ '1' with first enable_in
		gray_count_out <= {COUNTER_WIDTH{1'b0}};
	end
	
	else if (enable_in)
	begin
		binary_count <= binary_count + 1;
		gray_count_out <= {binary_count[COUNTER_WIDTH-1], binary_count[COUNTER_WIDTH-2:0] ^ binary_count[COUNTER_WIDTH-1:1]};
	end
	                                                                                                     
endmodule










