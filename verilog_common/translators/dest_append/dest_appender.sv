/*
 * function : puts the return dest/vc in a queue for the replies to take on their way out
 * author   : Mohamed S. Abdelfattah
 * date     : 13-OCT-2015
 */

module dest_appender
#(
	parameter ADDRESS_WIDTH = 4,
	parameter VC_ADDRESS_WIDTH = 1,
	parameter DEPTH = 12 //should be equal to the slave latency
)
(
    //clocks and reset
    input clk,
    input rst,
    
	//input port
	input    [ADDRESS_WIDTH-1:0] i_dst_in,
	input [VC_ADDRESS_WIDTH-1:0] i_vc_in,
	input                        i_valid_in,
    
    //output port
	output    [ADDRESS_WIDTH-1:0] o_dst_out,
	output [VC_ADDRESS_WIDTH-1:0] o_vc_out,
	input                         o_valid_in
);

//-------------------------------------------------------------------------
// Implementation
//-------------------------------------------------------------------------

reg fifo_full;
reg fifo_empty;

//synopsys translate off
always @ (*)
    if(fifo_full){
        $display("CHECK DEST APPENDER: ITS FIFO IS FULL AND SHOULDN'T BE IF THE DEPTH IS LARGER THAN SLAVE LATENCY!");
        $finish(1);
    }
//synopsys translate on

fifo_da
#(
    .WIDTH(ADDRESS_WIDTH+VC_ADDRESS_WIDTH),
    .DEPTH(DEPTH)
)
fifo_inst
(
    .clk(clk),
    .clear(rst),
    .i_data_in({i_dst_in,i_vc_in}),
    .i_write_en(i_valid_in),
    .i_full_out(fifo_full),
    .o_data_out({o_dst_out,o_vc_out}),
    .o_read_en(o_valid_in),
    .o_empty_out(fifo_empty)
);


endmodule


//-------------------------------------------------------------------------
// Helper modules
//-------------------------------------------------------------------------

/*
 * function : synchronous single-cycle fifo with "top" of queue always shown
 * author   : Mohamed S. Abdelfattah
 * date     : 26-AUG-2014
 * source   : asicworld.com
 */

module fifo_da
#(
	parameter WIDTH = 4,
	parameter DEPTH = 4
)
(
	//clocks and reset
	input wire clk,
	input wire clear,
	
	//write port
	input  wire [WIDTH-1:0] i_data_in,
	input  wire             i_write_en,
	output reg              i_full_out,

	//read port
	output reg  [WIDTH-1:0] o_data_out,
	input  wire             o_read_en,
	output reg              o_empty_out
);

//address width
localparam ADDRESS_WIDTH = $clog2(DEPTH);

//storage
reg [WIDTH-1:0] memory [DEPTH-1:0];

//pointers to head/tail and enables
wire [ADDRESS_WIDTH-1:0] next_read_addr, next_write_addr;
wire                     next_read_en, next_write_en;

//misc
wire equal_address, set_status, rst_status, preset_full, preset_empty;
reg status;

//--------------------------------------------------------------------------------------
// Implementation
//--------------------------------------------------------------------------------------

//data in
always @ (posedge clk)
	if (i_write_en & ~i_full_out)
		memory[next_write_addr] <= i_data_in;

//data out
always @ (*)
	o_data_out <= memory[next_read_addr];


//read/write address enables
assign next_write_en  = i_write_en & ~i_full_out;
assign next_read_en = o_read_en & ~o_empty_out;

//read address
gray_counter_da
	#(.COUNTER_WIDTH(ADDRESS_WIDTH))
read_address_counter
(
	.gray_count_out(next_read_addr),
	.enable_in(next_read_en),
	.clear_in(clear),
	.clk(clk)
);

//write address
gray_counter_da
	#(.COUNTER_WIDTH(ADDRESS_WIDTH))
write_address_counter
(
	.gray_count_out(next_write_addr),
	.enable_in(next_write_en),
	.clear_in(clear),
	.clk(clk)
);

//equal address check
assign equal_address = (next_read_addr == next_write_addr);

//quadrant select
assign set_status = (next_write_addr[ADDRESS_WIDTH-2] ~^ next_read_addr[ADDRESS_WIDTH-1]) &
                    (next_write_addr[ADDRESS_WIDTH-1] ^  next_read_addr[ADDRESS_WIDTH-2]);

assign rst_status = (next_write_addr[ADDRESS_WIDTH-2] ^  next_read_addr[ADDRESS_WIDTH-1]) &
                    (next_write_addr[ADDRESS_WIDTH-1] ~^ next_read_addr[ADDRESS_WIDTH-2]);

//status logic: are we going full (status = 1) or going empty (status = 0)
always @ *
	if(rst_status | clear)
		status = 1'b0;
	else if (set_status)
		status = 1'b1;

//full_out logic
assign preset_full = status & equal_address;

always @ (posedge clk, posedge preset_full)
	if(preset_full)
		i_full_out <= 1'b1;
	else
		i_full_out <= 1'b0;

//empty_out logic 
assign preset_empty = ~status & equal_address;

always @ (posedge clk, posedge preset_empty)
	if(preset_empty)
		o_empty_out <= 1'b1;
	else
		o_empty_out <= 1'b0;

endmodule

/*
 * function : gray counter
 * author   : Mohamed S. Abdelfattah
 * date     : 26-AUG-2014
 * source   : asicworld.com
 */	

module gray_counter_da
#(parameter   COUNTER_WIDTH = 4) 
(
	output reg  [COUNTER_WIDTH-1:0]    gray_count_out,  //'Gray' code count output.
	
	input wire                         enable_in,  //Count enable.
	input wire                         clear_in,   //Count reset.
					    
	input wire                         clk
);

//Internal connections  variables
reg    [COUNTER_WIDTH-1:0]  binary_count;

//-------------------------------------------------------
// Implementation
//-------------------------------------------------------
				            
always @ (posedge clk or posedge clear_in)
	
	if (clear_in)
	begin
		binary_count <= {COUNTER_WIDTH{1'b0}} + 1; //gray count begins @ '1' with first enable_in
		gray_count_out <= {COUNTER_WIDTH{1'b0}};
	end
	
	else if (enable_in)
	begin
		binary_count <= binary_count + 1;
		gray_count_out <= {binary_count[COUNTER_WIDTH-1], binary_count[COUNTER_WIDTH-2:0] ^ binary_count[COUNTER_WIDTH-1:1]};
	end
	                                                                                                     
endmodule

