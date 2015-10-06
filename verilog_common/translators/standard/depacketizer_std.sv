/*
 * function : take a data word/dest and insert proper packet and flit headers
 * author   : Mohamed S. Abdelfattah
 * date     : 26-AUG-2014
 */

module depacketizer_std
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
	
	output [WIDTH_DATA-1:0] data_out,
	output                  valid_out,
	input                   ready_in
);


//-------------------------------------------------------------------------
// Implementation
//-------------------------------------------------------------------------

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
	.data_in(data_in),
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
	.data_in(data_in),
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
	.data_in(data_in),
	.ready_out(ready_out),
	.data_out(data_out),
	.valid_out(valid_out),
	.ready_in(ready_in)
);
endgenerate

endmodule
