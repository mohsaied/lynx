/*
 * function : take in packets and spit out data only (strip control)
 * author   : Mohamed S. Abdelfattah
 * date     : 3-SEPT-2014
 */

module depacketizer_4_sub
#(
	parameter WIDTH_PKT = 36,
	parameter WIDTH_DATA = 12,
	parameter VC_ADDRESS_WIDTH = 1,
	parameter ADDRESS_WIDTH = 4
)
(
	input [WIDTH_PKT-1:0] data_in,
	output                ready_out,
	
	output [WIDTH_DATA-1:0] data_out,
	output                  valid_out,
	input                   ready_in
);

localparam WIDTH_FLIT = WIDTH_PKT/4;
localparam DATA_POS_HEAD = WIDTH_PKT - 3 - VC_ADDRESS_WIDTH - ADDRESS_WIDTH - 1;
localparam DATA_POS_B1   = WIDTH_PKT - WIDTH_FLIT - 3 - VC_ADDRESS_WIDTH - 1;
localparam DATA_POS_B2   = WIDTH_PKT - 2*WIDTH_FLIT - 3 - VC_ADDRESS_WIDTH - 1;
localparam DATA_POS_TAIL = WIDTH_PKT - 3*WIDTH_FLIT - 3 - VC_ADDRESS_WIDTH - 1;

localparam WIDTH_DATA_IDL = WIDTH_PKT - 3*4 -4*VC_ADDRESS_WIDTH - ADDRESS_WIDTH;
localparam EXTRA_BITS = WIDTH_DATA_IDL - WIDTH_DATA;

localparam VALID_POS_HEAD = WIDTH_PKT - 1;

wire [WIDTH_DATA_IDL-1:0] full_data;


//------------------------------------------------------------------------
// Implementation
//------------------------------------------------------------------------

assign ready_out = ready_in;
assign valid_out = data_in[VALID_POS_HEAD]===1'b1; //need this because of X's


//here we need to strip all the control bits and concat data back together


assign full_data = {
		data_in[DATA_POS_HEAD : 3*WIDTH_FLIT],
		data_in[DATA_POS_B1   : 2*WIDTH_FLIT],
		data_in[DATA_POS_B2   : 1*WIDTH_FLIT],
		data_in[DATA_POS_TAIL : 0]
};

assign data_out = full_data[WIDTH_DATA_IDL-1 -: WIDTH_DATA];



endmodule
