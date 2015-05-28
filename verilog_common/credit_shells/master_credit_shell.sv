/*
 * function : Logic to regulate the flow out of a master sharing a slave 
 *            based on the number of credits it has available
 * author   : Mohamed S. Abdelfattah
 * date     : 28-MAY-2015
 */

module master_credit_shell
#(
	parameter NUM_CREDITS = 8
)
(
	input clk,
	input rst,
    
    //the sending bundle tells us when it's valid, and we tell it when we're ready to send
    input  send_valid,
    output send_ready,
    
    //the receiving end tells us when a reply comes back, and we increment the credits
    //we also need to know if it's valid to be able to set the send ready correctly
    input receive_valid,
    input receive_ready
);

localparam COUNTER_WIDTH = $clog2(NUM_CREDITS);

//credit counter 
reg [COUNTER_WIDTH-1:0] credit_counter;

//-------------------------------------------------------
// Implementation
//-------------------------------------------------------

always @ (posedge clk)
begin
	if (rst)
	begin
        credit_counter = 0;
	end
	else
	begin
        if(send_valid)
            credit_counter = credit_counter - 1;
        if(receive_valid)
            credit_counter = credit_counter + 1;
	end
end

// assign the ready signal
assign send_ready = (credit_counter <= 1) && receive_ready;


endmodule















