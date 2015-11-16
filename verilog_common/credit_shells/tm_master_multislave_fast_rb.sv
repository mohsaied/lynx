/*
 * function : Logic to regulate the flow out of a master that is sending
 *            to multiple slaves -- a reorder buffer at the master makes 
 *            sure results are read in the proper order
 * author   : Mohamed S. Abdelfattah
 * date     : 05-NOV-2015
 */

module tm_master_multislave_fast_rb
#(
    parameter NUM_CREDITS = 8, //total
    parameter ADDRESS_WIDTH = 4,
    parameter VC_ADDRESS_WIDTH = 2,
    parameter NUM_VC = 4,
	parameter WIDTH_DATA_OUT = 36,
	parameter WIDTH_DATA_IN = 36,
    parameter WIDTH_TAG = 8
)
(
	input clk,
	input rst,
    
    //the sending bundle tells us when it's valid, and we tell it when we're ready to send
    input      send_valid_in,
    
    output reg [WIDTH_TAG-1:0] send_tag,
    
    input      send_ready_in,  //coming from pkt
    output reg send_ready_out, // going to module
    
    //recv end
    input       receive_valid_in,
    output reg  receive_valid_out,
    
    input       receive_ready_in,  //coming from module
    output reg  receive_ready_out,  //going to dpkt
    
    input [WIDTH_TAG-1:0] receive_tag,
    
    input      [WIDTH_DATA_IN-1 : 0] receive_data_in,  //coming from dpkts
    output reg [WIDTH_DATA_IN-1 : 0] receive_data_out  //going to module
);

assign receive_ready_out = receive_ready_in;

integer i;

localparam COUNTER_WIDTH = $clog2(NUM_CREDITS+1)+1;

//credit counter 
reg [COUNTER_WIDTH-1:0] credit_counter;

reg [WIDTH_TAG-1:0] curr_tag;

reg found_data;

//-------------------------------------------------------
// Implementation
//-------------------------------------------------------

//at the output, we will store incoming data in a buffer, and we are always checking the tag of the buffer, if it matches our current tag, then we found our data, otherwise, we'll increment the read pointer until we find it

reg [WIDTH_DATA_IN+WIDTH_TAG-1:0] storage_buffer [0:NUM_CREDITS];
reg [NUM_CREDITS:0] storage_buffer_valid;
reg [WIDTH_DATA_IN-1:0] curr_read_data;
reg [WIDTH_TAG-1:0] curr_read_tag;
reg [COUNTER_WIDTH - 1 : 0] next_read_addr;
reg [COUNTER_WIDTH - 1 : 0] next_write_addr;

//as data is coming in, store in an available storage buffer location
always@(posedge clk)
begin
    if(rst)
    begin
        for(i=0;i<=NUM_CREDITS;i++)
        begin
            storage_buffer[i] = 0;
            storage_buffer_valid[i] = 0;
        end
        next_read_addr = 0;
        next_write_addr = 0;
        curr_tag = 0;
    end
    else 
    begin
        
        //write incoming data to the write addr
        if(receive_valid_in)
        begin
            next_write_addr = receive_tag % (NUM_CREDITS+1);
            //synposys translate off
            if(storage_buffer_valid[next_write_addr])
            begin
                $display("HASH COLLISION IN ROB ADDR %d!",next_write_addr);
                $finish(1);
            end
            //synposys translate on
            storage_buffer[next_write_addr] = {receive_tag,receive_data_in};
            storage_buffer_valid[next_write_addr] = 1;
        end
        
        /*
        //decide on next place to write -- look for empty location
        if(storage_buffer_valid[next_write_addr])
        begin
            for(i=0;i<=NUM_CREDITS;i++)
            begin
                if(~storage_buffer_valid[i])
                begin
                    next_write_addr = i;
                end
            end
        end
        
        //decide on next place to read -- a valid location
        
        next_read_addr = next_read_addr + 1;
        if(next_read_addr == NUM_CREDITS+1)
            next_read_addr = 0;
        for(i=0;i<=NUM_CREDITS;i++)
        begin
            if(~storage_buffer_valid[next_read_addr])
            begin
                next_read_addr = next_read_addr + 1;
                if(next_read_addr == NUM_CREDITS+1)
                    next_read_addr = 0;
            end
        end
        */
        
        next_read_addr = curr_tag % (NUM_CREDITS+1);
        
        found_data = 0;
        receive_valid_out = 0;
        //read incoming data from the read addr
        if(storage_buffer_valid[next_read_addr])
        begin
            {curr_read_tag,curr_read_data} = storage_buffer[next_read_addr];
            
            storage_buffer_valid[next_read_addr] = 0;
            curr_tag = curr_tag + 1;
            found_data = 1;
            receive_data_out = curr_read_data;
            receive_valid_out = 1;
        end
        
    end
end

/*
//store the tag in a buffer to know which one to fetch
fifo_msf
#(
    .WIDTH(WIDTH_TAG),
    .DEPTH(2**($clog2(NUM_CREDITS*2))) 
)
fifo_fast
(
    .clk(clk),
    .clear(rst),
    .i_data_in(send_tag),
    .i_write_en(send_valid_in),
    .i_full_out(),
    .o_data_out(curr_tag),
    .o_read_en(found_data),
    .o_empty_out()
);
*/

//increment the tag id whenever we are sending valid data
always@ (posedge clk)
begin
    if(rst)
        send_tag = 0;
    else if(send_valid_in)
        send_tag = send_tag + 1;
end


//track number of credits to know if we can send
always @ (posedge clk)
begin
	if (rst)
	begin
        credit_counter = NUM_CREDITS+1;
	end
	else
	begin
        if(send_valid_in===1'b1)
        begin
            //synopsys translate off
            if(credit_counter == 0) begin
                $display("CREDIT COUNTER UNDERFLOW ERROR!");
                $finish(1);
            end
            //synopsys translate on
            credit_counter = credit_counter - 1;
        end
        if(receive_valid_out===1'b1) 
        begin
            //synopsys translate off
            if(credit_counter == NUM_CREDITS+1) begin
                $display("CREDIT COUNTER OVERFLOW ERROR!");
                $finish(1);
            end
            //synopsys translate on
            credit_counter = credit_counter + 1;
        end
	end
end

// assign the ready signal
assign send_ready_out = (credit_counter > 1) && send_ready_in===1'b1;



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










