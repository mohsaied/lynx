/*
 * function : take a data word/dest and insert proper packet and flit headers
 * author   : Mohamed S. Abdelfattah
 * date     : 26-AUG-2014
 */

module packetizer_vc
#(
	parameter ADDRESS_WIDTH = 4,
	parameter VC_ADDRESS_WIDTH = 1,
	parameter WIDTH_IN  = 12,
	//parameter WIDTH_OUT = ((WIDTH_IN + 3*4 + ADDRESS_WIDTH + 4*VC_ADDRESS_WIDTH + 3)/4) * 4 
	parameter WIDTH_OUT = 36,
    parameter PACKETIZER_WIDTH = 1,
    parameter NUM_DEST = 4,        //number of destinations
	parameter  [ADDRESS_WIDTH-1:0] DEST [0:NUM_DEST-1] = '{NUM_DEST{1}}, //router index that this vc_table sends to
	parameter [VC_ADDRESS_WIDTH-1:0] VC [0:NUM_DEST-1] = '{NUM_DEST{1}}  //VC of each destination
)
(
	//input port
	input    [WIDTH_IN-1:0]      data_in,
	input                        valid_in,
	input  [  ADDRESS_WIDTH-1:0] dst_in,
	input [VC_ADDRESS_WIDTH-1:0] vc_in,
	output                       ready_out,

	//output port
	output [WIDTH_OUT-1:0] data_out,
	output                 valid_out,
	input                  ready_in
);

wire [VC_ADDRESS_WIDTH-1:0] vc_wire;


//-------------------------------------------------------------------------
// Implementation
//-------------------------------------------------------------------------

//choose the packetizer based on PACKETIZER_WIDTH parameter
generate

if(PACKETIZER_WIDTH == 1)
packetizer_1_sub
#(
    .ADDRESS_WIDTH(ADDRESS_WIDTH),
	.VC_ADDRESS_WIDTH(VC_ADDRESS_WIDTH),
	.WIDTH_IN(WIDTH_IN),
	.WIDTH_OUT(WIDTH_OUT)
)
pk1_1_sub
(
	.data_in(data_in),
	.valid_in(valid_in),
	.dst_in(dst_in),
	.vc_in(vc_in),
	.ready_out(ready_out),
	.data_out(data_out),
	.valid_out(valid_out),
	.ready_in(ready_in)
);
else if(PACKETIZER_WIDTH == 2)
packetizer_2_sub
#(
    .ADDRESS_WIDTH(ADDRESS_WIDTH),
	.VC_ADDRESS_WIDTH(VC_ADDRESS_WIDTH),
	.WIDTH_IN(WIDTH_IN),
	.WIDTH_OUT(WIDTH_OUT)
)
pk1_2_sub
(
	.data_in(data_in),
	.valid_in(valid_in),
	.dst_in(dst_in),
	.vc_in(vc_in),
	.ready_out(ready_out),
	.data_out(data_out),
	.valid_out(valid_out),
	.ready_in(ready_in)
);
else if(PACKETIZER_WIDTH == 3)
packetizer_3_sub
#(
    .ADDRESS_WIDTH(ADDRESS_WIDTH),
	.VC_ADDRESS_WIDTH(VC_ADDRESS_WIDTH),
	.WIDTH_IN(WIDTH_IN),
	.WIDTH_OUT(WIDTH_OUT)
)
pk1_3_sub
(
	.data_in(data_in),
	.valid_in(valid_in),
	.dst_in(dst_in),
	.vc_in(vc_in),
	.ready_out(ready_out),
	.data_out(data_out),
	.valid_out(valid_out),
	.ready_in(ready_in)
);
else if(PACKETIZER_WIDTH == 4)
packetizer_4_sub
#(
    .ADDRESS_WIDTH(ADDRESS_WIDTH),
	.VC_ADDRESS_WIDTH(VC_ADDRESS_WIDTH),
	.WIDTH_IN(WIDTH_IN),
	.WIDTH_OUT(WIDTH_OUT)
)
pk1_4_sub
(
	.data_in(data_in),
	.valid_in(valid_in),
	.dst_in(vc_in),
	.vc_in(vc_wire),
	.ready_out(ready_out),
	.data_out(data_out),
	.valid_out(valid_out),
	.ready_in(ready_in)
);
endgenerate

endmodule
