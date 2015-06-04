/*
 * function : Logic to regulate the flow out of a master sharing a slave 
 *            based on the number of credits it has available
 * author   : Mohamed S. Abdelfattah
 * date     : 28-MAY-2015
 */

module tm_master_credit
#(
	parameter NUM_CREDITS = 8
)
(
	input clk,
	input rst,
    
    //the sending bundle tells us when it's valid, and we tell it when we're ready to send
    input  send_valid,
    output send_ready_out,
    input  send_ready_in,
    
    //the receiving end tells us when a reply comes back, and we increment the credits
    //we also need to know if it's valid to be able to set the send ready correctly
    input receive_valid
);

localparam COUNTER_WIDTH = $clog2(NUM_CREDITS+1)+1;

//credit counter 
reg [COUNTER_WIDTH-1:0] credit_counter;

//-------------------------------------------------------
// Implementation
//-------------------------------------------------------

always @ (posedge clk)
begin
	if (rst)
	begin
        credit_counter = NUM_CREDITS+1;
	end
	else
	begin
        if(send_valid)
        begin
            //synopsys translate off
            if(credit_counter == 0) begin
                $display("CREDIT COUNTER UNDERFLOW ERROR!");
                $finish(1);
            end
            //synopsys translate on
            credit_counter = credit_counter - 1;
        end
        if(receive_valid) 
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
assign send_ready_out = (credit_counter > 1) && send_ready_in;


endmodule















