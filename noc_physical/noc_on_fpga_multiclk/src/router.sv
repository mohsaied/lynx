module router
#(
	parameter WIDTH = 520
)
(
	input                  clk,
	input      [WIDTH-1:0] inputs /* synthesis noprune  */,
	output reg [WIDTH-1:0] outputs /* synthesis noprune  */
);

always @ (posedge clk)
begin
	outputs[WIDTH-1:0] <= inputs[WIDTH-1:0];
end

endmodule