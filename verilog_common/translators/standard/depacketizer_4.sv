/*
 * function : take in packets and spit out data only (strip control)
 * author   : Mohamed S. Abdelfattah
 * date     : 3-SEPT-2014
 */

module depacketizer_4
#(
	parameter WIDTH_PKT = 36,
	parameter WIDTH_DATA = 12,
	parameter VC_ADDRESS_WIDTH = 1,
	parameter ADDRESS_WIDTH = 4
)
(
	input [WIDTH_PKT-1:0] i_packet_in,
	input                 i_valid_in,
	output                i_ready_out,
	
	output [WIDTH_DATA-1:0] o_data_out,
	output                  o_valid_out,
	input                   o_ready_in
);

localparam WIDTH_FLIT = WIDTH_PKT/4;
localparam DATA_POS_HEAD = WIDTH_PKT - 3 - VC_ADDRESS_WIDTH - ADDRESS_WIDTH - 1;
localparam DATA_POS_B1   = WIDTH_PKT - WIDTH_FLIT - 3 - VC_ADDRESS_WIDTH - 1;
localparam DATA_POS_B2   = WIDTH_PKT - 2*WIDTH_FLIT - 3 - VC_ADDRESS_WIDTH - 1;
localparam DATA_POS_TAIL = WIDTH_PKT - 3*WIDTH_FLIT - 3 - VC_ADDRESS_WIDTH - 1;

localparam WIDTH_DATA_IDL = WIDTH_PKT - 3*4 -4*VC_ADDRESS_WIDTH - ADDRESS_WIDTH;
localparam EXTRA_BITS = WIDTH_DATA_IDL - WIDTH_DATA;

wire [WIDTH_DATA_IDL-1:0] full_data;


//------------------------------------------------------------------------
// Implementation
//------------------------------------------------------------------------

assign i_ready_out = o_ready_in;
assign o_valid_out = i_valid_in;


//here we need to strip all the control bits and concat data back together


assign full_data = {
		i_packet_in[DATA_POS_HEAD : 3*WIDTH_FLIT],
		i_packet_in[DATA_POS_B1   : 2*WIDTH_FLIT],
		i_packet_in[DATA_POS_B2   : 1*WIDTH_FLIT],
		i_packet_in[DATA_POS_TAIL : 0]
};

assign o_data_out = full_data[WIDTH_DATA_IDL-1 -: WIDTH_DATA];



endmodule
