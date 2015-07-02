/*
 * function : Qsys slave with debug messages for perf eval
 * author   : Mohamed S. Abdelfattah
 * date     : 9-JUNE-2015
 */

module qsys_slave
#(
	parameter            WIDTH = 32, //data width
    parameter [7:0]     SRC_ID = 0,  //unique id associated with each src
    parameter [7:0]     SNK_ID = 1,  //unique id associated with each src
    parameter       ADDR_WIDTH = 30  //address width doesn't matter
)
(
	input clk,
	input rst,
    output done,
    
    input  wire      [WIDTH-1:0] writedata,
    input  wire [ADDR_WIDTH-1:0] address,
    input  wire                  write,
    input  wire                  read,
    output wire      [WIDTH-1:0] readdata,
    output wire                  readdatavalid,
    
    input  [WIDTH+ADDR_WIDTH+1:0] v_inputs,
    output [WIDTH+ADDR_WIDTH+1:0] v_outputs
    
);

wire [ADDR_WIDTH-1:0] fake_address ;
wire fake_read ;

filler
#(
    .NOC_WIDTH(WIDTH+ADDR_WIDTH+2)
)
filler_in
(
    .clk(clk),
    .rst(rst),
    .inputs(v_inputs),
    .outputs({readdata,readdatavalid,fake_read,fake_address})
);

filler
#(
    .NOC_WIDTH(WIDTH+ADDR_WIDTH+2)
)
filler_out
(
    .clk(clk),
    .rst(rst),
    .inputs({writedata,address&fake_address,write,read&fake_read}),
    .outputs(v_outputs)
);

endmodule















