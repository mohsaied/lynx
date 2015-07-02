/* 
 * function : general top-level file for noc physical interconnection
 * author   : Mohamed S. Abdelfattah
 * date     : 04-Jun-2015
 */

module noc_on_fpga
#( 
    parameter NOC_WIDTH = 600, // can support up to 800 
    parameter NOC_NODES = 16
)
(
	input clk, //sys clk
	input rst, //active-low async reset
	
	input  [NOC_WIDTH-1:0] v_inputs [0:NOC_NODES-1],
	output [NOC_WIDTH-1:0] v_outputs [0:NOC_NODES-1]
);


reg [NOC_WIDTH-1:0] inputs [0:NOC_NODES-1] /* synthesis noprune  */;
reg [NOC_WIDTH-1:0] outputs [0:NOC_NODES-1] /* synthesis noprune  */;

//The NoC!
genvar i;
generate
    for(i=0;i<NOC_NODES;i=i+1)
    begin:node
    router
    #(
        .WIDTH(NOC_WIDTH)
    )
    router_inst
    (
        .clk(clk),
        .inputs(inputs[i]),
        .outputs(outputs[i])
    );
    end
endgenerate

/*
always @ (posedge clk)
begin
	v_outputs = outputs;
	inputs = v_inputs;
end
*/

localparam MOD_WIDTH = 320;
localparam NUM_MODS = 10;

//The design!
generate
for(i=0;i<NUM_MODS;i=i+1)
begin:mod
    
	 filler
    #(
        .NOC_WIDTH(MOD_WIDTH)
    )
    filler_in 
    (
        .clk(clk), 
        .inputs(v_inputs[i][NOC_WIDTH-1 -: MOD_WIDTH]),
        .outputs(inputs[i][NOC_WIDTH-1 -: MOD_WIDTH])
    );
    
	 filler
    #(
        .NOC_WIDTH(MOD_WIDTH)
    )
    filler_out
    (
        .clk(clk),
        .inputs(outputs[i][NOC_WIDTH-1 -: MOD_WIDTH]),
        .outputs(v_outputs[i][NOC_WIDTH-1 -: MOD_WIDTH])
    );
	 
end	 
endgenerate

endmodule
