/*
 * function : take a data word/dest and insert proper packet and flit headers and also return dest and vc
 * author   : Mohamed S. Abdelfattah
 * date     : 13-OCT-2015
 */

module packetizer_da
#(
	parameter ADDRESS_WIDTH = 4,
	parameter VC_ADDRESS_WIDTH = 1,
	parameter WIDTH_IN  = 12,
	//parameter WIDTH_OUT = ((WIDTH_IN + 3*4 + ADDRESS_WIDTH + 4*VC_ADDRESS_WIDTH + 3)/4) * 4 
	parameter WIDTH_OUT = 36,
    parameter PACKETIZER_WIDTH = 1,
    parameter [ADDRESS_WIDTH-1:0] DEST,
    parameter [VC_ADDRESS_WIDTH-1:0] VC
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

//-------------------------------------------------------------------------
// Implementation
//-------------------------------------------------------------------------

localparam WIDTH_IN_MOD = WIDTH_IN + ADDRESS_WIDTH + VC_ADDRESS_WIDTH;

//synopsys translate off
always @ (*)
    if( (WIDTH_IN_MOD+3+ADDRESS_WIDTH+VC_ADDRESS_WIDTH) > WIDTH_OUT){
        $display("PACKETIZER_DA needs to pack %d bits, but only has %d bits",WIDTH_IN_MOD+3+ADDRESS_WIDTH+VC_ADDRESS_WIDTH,WIDTH_OUT);
        $finish(1);
    }
//synopsys translate on

reg [WIDTH_IN_MOD-1:0] data_dst_vc = {DEST,VC,data_in};


//choose the packetizer based on PACKETIZER_WIDTH parameter
generate

if(PACKETIZER_WIDTH == 1)
packetizer_1_sub
#(
    .ADDRESS_WIDTH(ADDRESS_WIDTH),
	.VC_ADDRESS_WIDTH(VC_ADDRESS_WIDTH),
	.WIDTH_IN(WIDTH_IN_MOD),
	.WIDTH_OUT(WIDTH_OUT)
)
pk1_1_sub
(
	.data_in(data_dst_vc),
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
	.WIDTH_IN(WIDTH_IN_MOD),
	.WIDTH_OUT(WIDTH_OUT)
)
pk1_2_sub
(
	.data_in(data_dst_vc),
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
	.WIDTH_IN(WIDTH_IN_MOD),
	.WIDTH_OUT(WIDTH_OUT)
)
pk1_3_sub
(
	.data_in(data_dst_vc),
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
	.WIDTH_IN(WIDTH_IN_MOD),
	.WIDTH_OUT(WIDTH_OUT)
)
pk1_4_sub
(
	.data_in(data_dst_vc),
	.valid_in(valid_in),
	.dst_in(dst_in),
	.vc_in(vc_in),
	.ready_out(ready_out),
	.data_out(data_out),
	.valid_out(valid_out),
	.ready_in(ready_in)
);
endgenerate

endmodule
