/*
 * function : Qsys slave with debug messages for perf eval
 * author   : Mohamed S. Abdelfattah
 * date     : 9-JUNE-2015
 */

module qsys_master
#(
	parameter           WIDTH = 32, //data width
    parameter [7:0]    SRC_ID =  2, //unique id associated with each src
    parameter [7:0]    SNK_ID =  3, //unique id associated with each sink
    parameter [7:0]    DST_ID =  1, //id associated with the destination
    parameter      ADDR_WIDTH = 32  //address width doesn't matter
)
(
	input clk,
	input rst,
    output done,
    
    output wire      [WIDTH-1:0] writedata ,
    output wire [ADDR_WIDTH-1:0] address,
    output wire                  write,
    output wire                  read,
    input  wire      [WIDTH-1:0] readdata,
    input  wire                  readdatavalid,
    input  wire                  waitrequest,
    
    input  [WIDTH+ADDR_WIDTH+1:0] v_inputs,
    output [WIDTH+ADDR_WIDTH+1:0] v_outputs
);

filler
#(
    .NOC_WIDTH(WIDTH+ADDR_WIDTH+2)
)
filler_in
(
    .clk(clk),
    .rst(rst),
    .inputs(v_inputs),
    .outputs({writedata,address,write,read})
);

filler
#(
    .NOC_WIDTH(WIDTH+ADDR_WIDTH+2)
)
filler_out
(
    .clk(clk),
    .rst(rst),
    .inputs({readdata,readdatavalid,waitrequest,readdata[WIDTH-1 -: ADDR_WIDTH]}),
    .outputs(v_outputs)
);


endmodule















