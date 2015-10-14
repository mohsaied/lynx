/*
 * function : take a data word and insert proper packet and flit headers, also parses out the dest and VC to pass to the pkt
 * author   : Mohamed S. Abdelfattah
 * date     : 13-OCT-2015
 */

module depacketizer_da
#(
	parameter WIDTH_PKT = 36,
	parameter WIDTH_DATA = 12,
	parameter VC_ADDRESS_WIDTH = 1,
	parameter ADDRESS_WIDTH = 4,
    parameter DEPACKETIZER_WIDTH = 4
)
(
	input [WIDTH_PKT-1:0] data_in,
	output                ready_out,
	
	output       [WIDTH_DATA-1:0] data_out,
	output    [ADDRESS_WIDTH-1:0] dst_out,
	output [VC_ADDRESS_WIDTH-1:0] vc_out,
	output                        valid_out,
	input                         ready_in
);


//-------------------------------------------------------------------------
// Implementation
//-------------------------------------------------------------------------

localparam RETURN_DEST_POS = WIDTH_PKT - 3 - ADDRESS_WIDTH - VC_ADDRESS_WIDTH -1;
localparam RETURN_VC_POS = RETURN_DEST_POS - ADDRESS_WIDTH;
localparam ORIG_DATA_POS = RETURN_VC_POS - VC_ADDRESS_WIDTH;

//parse out the return dest and vc
assign dst_out = data_in[RETURN_DEST_POS -: ADDRESS_WIDTH];
assign vc_out  = data_in[RETURN_VC_POS -: VC_ADDRESS_WIDTH];

reg [WIDTH_PKT-1:0] data_in_after_extraction;
assign data_in_after_extraction = {data_in[WIDTH_PKT-1 : RETURN_DEST_POS+1], data_in[ORIG_DATA_POS : 0], {ADDRESS_WIDTH{1'b0}}, {VC_ADDRESS_WIDTH{1'b0}} };

//choose the depacketizer based on DEPACKETIZER_WIDTH parameter
generate

if(DEPACKETIZER_WIDTH == 1)
depacketizer_1_sub
#(
    .ADDRESS_WIDTH(ADDRESS_WIDTH),
	.VC_ADDRESS_WIDTH(VC_ADDRESS_WIDTH),
	.WIDTH_PKT(WIDTH_PKT),
	.WIDTH_DATA(WIDTH_DATA)
)
pk1_1_sub
(
	.data_in(data_in_after_extraction),
	.ready_out(ready_out),
	.data_out(data_out),
	.valid_out(valid_out),
	.ready_in(ready_in)
);
else if(DEPACKETIZER_WIDTH == 2)
depacketizer_2_sub
#(
    .ADDRESS_WIDTH(ADDRESS_WIDTH),
	.VC_ADDRESS_WIDTH(VC_ADDRESS_WIDTH),
	.WIDTH_PKT(WIDTH_PKT),
	.WIDTH_DATA(WIDTH_DATA)
)
pk1_2_sub
(
	.data_in(data_in_after_extraction),
	.ready_out(ready_out),
	.data_out(data_out),
	.valid_out(valid_out),
	.ready_in(ready_in)
);
else if(DEPACKETIZER_WIDTH == 4)
depacketizer_4_sub
#(
    .ADDRESS_WIDTH(ADDRESS_WIDTH),
	.VC_ADDRESS_WIDTH(VC_ADDRESS_WIDTH),
	.WIDTH_PKT(WIDTH_PKT),
	.WIDTH_DATA(WIDTH_DATA)
)
pk1_4_sub
(
	.data_in(data_in_after_extraction),
	.ready_out(ready_out),
	.data_out(data_out),
	.valid_out(valid_out),
	.ready_in(ready_in)
);
endgenerate

endmodule
