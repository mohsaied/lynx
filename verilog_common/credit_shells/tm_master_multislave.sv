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
    parameter VC_ADDRESS_WIDTH = 2
)
(
	input clk,
	input rst,
    
    //the sending bundle tells us when it's valid, and we tell it when we're ready to send
    input                           send_valid,
    input     [ADDRESS_WIDTH-1 : 0] send_dest,
    input  [VC_ADDRESS_WIDTH-1 : 0] send_vc,
    input                           send_ready_in,  //coming from NoC
    output                          send_ready_out, // going to module
    
    //the receiving end tells us when a reply comes back, and we increment the credits
    //we also need to know if it's valid to be able to set the send ready correctly
    input receive_valid
);

localparam COUNTER_WIDTH = $clog2(NUM_CREDITS+1)+1;

//counter to keep track of the number of outstanding requests
reg [COUNTER_WIDTH-1:0] num_outstanding;

//current dest/vc
reg [ADDRESS_WIDTH+VC_ADDRESS_WIDTH-1 : 0] prev_dest;
wire [ADDRESS_WIDTH+VC_ADDRESS_WIDTH-1 : 0] curr_dest;
assign curr_dest = {send_dest,send_vc};

//stall signal because of switching
reg stall_switch;

//-------------------------------------------------------
// Implementation
//-------------------------------------------------------

//keep track of the number of outstanding requests
always @ (posedge clk)
begin
	if (rst)
	begin
        num_outstanding = 0;
	end
	else
	begin
        if(send_valid===1'b1)
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

//did we switch the slave we're sending to?
always  @ (posedge clk)
begin
    if (rst)
    begin
        prev_dest = 0;
        stall_switch = 0;
    end
    else
    begin
        if (curr_dest !== prev_dest)
        begin
            if(num_outstanding === 0)
            begin
                prev_dest = curr_dest;
                stall_switch = 0;
            end
            else
            begin
                stall_switch = 1;
            end
        end
    end
end

// assign the ready signal
assign send_ready_out = (stall_switch === 0) && (num_outstanding < NUM_CREDITS-1) && send_ready_in===1'b1;


endmodule















