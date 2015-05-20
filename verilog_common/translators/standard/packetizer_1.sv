/*
 * function : take a data word/dest and insert proper packet and flit headers
 * author   : Mohamed S. Abdelfattah
 * date     : 26-AUG-2014
 */

module packetizer_1
#(
	parameter ADDRESS_WIDTH = 4,
	parameter VC_ADDRESS_WIDTH = 1,
	parameter WIDTH_IN  = 12,
	//parameter WIDTH_OUT = ((WIDTH_IN + 3*4 + ADDRESS_WIDTH + 4*VC_ADDRESS_WIDTH + 3)/4) * 4 
	parameter WIDTH_OUT = 36,
	parameter [VC_ADDRESS_WIDTH-1:0] ASSIGNED_VC = 0
)
(
	//input port
	input [WIDTH_IN-1:0]      data_in,
	input                     valid_in,
	input [ADDRESS_WIDTH-1:0] dst_in,
	output                    ready_out,

	//output port
	output [WIDTH_OUT-1:0] data_out,
	output                 valid_out,
	input                  ready_in
);

//ideal flit widths
localparam FLIT_1_WIDTH_IDL = WIDTH_OUT/2 - 3 - ADDRESS_WIDTH - VC_ADDRESS_WIDTH;

//actual flit widths
localparam FLIT_1_VALID     = 1;
localparam FLIT_1_WIDTH_ACT = FLIT_1_WIDTH_IDL > WIDTH_IN ? WIDTH_IN : FLIT_1_WIDTH_IDL;
localparam FLIT_1_PADDING   = FLIT_1_WIDTH_IDL - FLIT_1_WIDTH_ACT;
localparam REM_FROM_1       = WIDTH_IN - FLIT_1_WIDTH_ACT; 

wire [FLIT_1_WIDTH_ACT-1:0] flit_1_data;


//-------------------------------------------------------------------------
// Implementation
//-------------------------------------------------------------------------

assign valid_out = valid_in;
assign ready_out = ready_in;

assign flit_1_data = FLIT_1_VALID ? data_in[WIDTH_IN-1 -: FLIT_1_WIDTH_ACT] : 0;

assign data_out = {
	(valid_in & FLIT_1_VALID),
	1'b1,
	1'b1,
	ASSIGNED_VC,
	dst_in,
	flit_1_data,
	{FLIT_1_PADDING{1'b0}}
};

endmodule
