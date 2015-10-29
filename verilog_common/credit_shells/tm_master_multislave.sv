/*
 * function : Logic to regulate the flow out of a master that is sending
 *            to multiple slaves -- we stall_switch the master when it switches to another slave
 * author   : Mohamed S. Abdelfattah
 * date     : 28-Oct-2015
 */

module tm_master_multislave
#(
    parameter NUM_CREDITS = 32,
    parameter ADDRESS_WIDTH = 4,
    parameter VC_ADDRESS_WIDTH = 2,
	parameter WIDTH_NOC = 36
)
(
	input clk,
	input rst,
    
    //the sending bundle tells us when it's valid, and we tell it when we're ready to send
    input                           send_valid_in,
    output reg                      send_valid_out,
    input         [WIDTH_NOC-1 : 0] send_data_in,
    output reg    [WIDTH_NOC-1 : 0] send_data_out,
    input     [ADDRESS_WIDTH-1 : 0] send_dest,
    input  [VC_ADDRESS_WIDTH-1 : 0] send_vc,
    input                           send_ready_in,  //coming from NoC
    output reg                      send_ready_out, // going to module
    
    //the receiving end tells us when a reply comes back, and we increment the credits
    //we also need to know if it's valid to be able to set the send ready correctly
    input receive_valid
);

localparam COUNTER_WIDTH = $clog2(NUM_CREDITS+1)+1;

//counter to keep track of the number of outstanding requests
reg [COUNTER_WIDTH-1:0] num_outstanding;

//current dest/vc
reg [ADDRESS_WIDTH+VC_ADDRESS_WIDTH-1 : 0] sending_dst;
reg [ADDRESS_WIDTH+VC_ADDRESS_WIDTH-1 : 0] main_buffer_dst;
reg [ADDRESS_WIDTH+VC_ADDRESS_WIDTH-1 : 0] overflow_buffer_dst;

//stall signal because of switching
reg stall_switch;

//skid buffer to store the data when we aren't allowed to send it
reg [WIDTH_NOC-1 : 0] main_buffer;
reg [WIDTH_NOC-1 : 0] overflow_buffer;
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
        stall_switch <= 0;
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
                main_buffer_dst <= {send_vc,send_dest};
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
                overflow_buffer_dst <= {send_vc,send_dest};
                send_ready_out <= 0;
            end
        end
        
        //read into main buffer
        else if (send_valid_in & ~main_buffer_valid)
        begin
            main_buffer <= send_data_in;
            main_buffer_valid <= 1;
            main_buffer_dst <= {send_vc,send_dest};
            send_ready_out <= 0;
        end
        
        //read into overflow buffer
        else if (send_valid_in & main_buffer_valid)
        begin
            //synposys translate off
            if(overflow_buffer_valid===1)
            begin
                $display("CANT ACCEPT MORE DATA!");
                $stop(1);
            end
            //synposys translate on
            overflow_buffer <= send_data_in;
            overflow_buffer_valid <= 1;
            overflow_buffer_dst <= {send_vc,send_dest};
            send_ready_out <= 0;
        end
        
        if(main_buffer_dst == sending_dst)
        begin
            stall_switch = 0;
        end
        else if(num_outstanding != 0)
        begin
            stall_switch = 1;
        end
        else if(num_outstanding == 0)
        begin
            stall_switch = 0;
        end
        
        //are we going to output data?
        //yes if we aren't switching slaves and have enough credits
        if(main_buffer_valid && ~stall_switch && num_outstanding < NUM_CREDITS && send_ready_in)
        begin
            send_data_out <= main_buffer;
            send_valid_out <= 1;
            main_buffer <= 0;
            main_buffer_valid <= 0;
            sending_dst <= main_buffer_dst;
            send_ready_out <= ~overflow_buffer_valid;
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
        num_outstanding = 0;
        predict_send = 0;
	end
	else
	begin
        predict_send = main_buffer_valid && stall_switch==0 && num_outstanding < NUM_CREDITS && send_ready_in==1;
    
        if(predict_send===1'b1)
        begin
            //synopsys translate off
            if(num_outstanding === NUM_CREDITS) begin
                $display("MULTISLAVE COUNTER OVERFLOW ERROR!");
                $finish(1);
            end
            //synopsys translate on
            num_outstanding = num_outstanding + 1;
        end
        if(receive_valid===1'b1) 
        begin
            //synopsys translate off
            if(num_outstanding === 0) begin
                $display("MULTISLAVE COUNTER UNDERFLOW ERROR!");
                $finish(1);
            end
            //synopsys translate on
            num_outstanding = num_outstanding - 1;
        end
	end
end
/*
//did we switch the slave? need to set the "stall_switch" signal
always @ (posedge clk)
begin
	if (rst)
	begin
        stall_switch <= 0;
	end
	else
	begin
        if(main_buffer_dst == sending_dst)
        begin
            stall_switch = 0;
        end
        else if(num_outstanding > 0)
        begin
            stall_switch = 1;
        end
	end
end
*/

endmodule















