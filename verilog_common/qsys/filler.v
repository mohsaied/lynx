/*
 * function : dummy module to take up space on the fpga; used to evaluate expected frequency of a system with any kind of interconnect
 * author   : Mohamed S. Abdelfattah
 * date     : 15-JUNE-2015
 */

module filler
#(
	parameter NOC_WIDTH = 350  //must be divisible by 50 to work correctly
)
(
	input clk,
	input rst,
	
	input  [NOC_WIDTH-1:0] inputs,
	output [NOC_WIDTH-1:0] outputs
);

integer i;

//-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#
// STAGE 1: REGISTERS
//-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#

reg [NOC_WIDTH-1:0] stage_1_reg;

always @ (posedge clk)
begin
	if(rst)
		stage_1_reg = 0;
	else
		stage_1_reg = inputs;
end

//-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#
// STAGE 2: SIMPLE ARITHMETIC
//-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#

localparam NUM_ARITH = 20;

reg [NOC_WIDTH-1:0] stage_2_reg [0:NUM_ARITH];
	
always @ (posedge clk)
begin
	if(rst)
		stage_2_reg[0] = 0;
	else
		stage_2_reg[0] = stage_1_reg;
end
	
genvar j;
generate
for(j=0;j<NUM_ARITH;j=j+1)
begin: ARITH

	always @ (posedge clk)
	begin
		if(rst)
			stage_2_reg[j+1] = 0;
		else
		begin
			for(i = 0; i < NOC_WIDTH/20; i=i+1)
			begin
				stage_2_reg[j+1][NOC_WIDTH-1-i*10   -: 10] = stage_2_reg[j][NOC_WIDTH/2-1-i*10 -: 10] + stage_2_reg[j][NOC_WIDTH-1-i*10 -: 10];
				stage_2_reg[j+1][NOC_WIDTH/2-1-i*10 -: 10] = stage_2_reg[j][NOC_WIDTH/2-1-i*10 -: 10] - stage_2_reg[j][NOC_WIDTH-1-i*10 -: 10];
			end
		end
	end
	
end
endgenerate


//-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#
// STAGE 3: BLOCK MEMORY
//-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#

localparam NUM_RAM_STAGES = 5;
localparam NUM_RAM = NOC_WIDTH/50;

reg  [NOC_WIDTH-1:0] stage_3_reg;
wire [NOC_WIDTH-1:0] stage_3_wire [0:NUM_RAM_STAGES]; 
	
always @ (posedge clk)
begin
	if(rst)
		stage_3_reg = 0;
	else
		stage_3_reg = stage_2_reg[NUM_ARITH];
end 

assign stage_3_wire[0] = stage_3_reg;
	
genvar k;
generate
for(k=0;k<NUM_RAM_STAGES;k=k+1)
begin:MEM_STAGE

	for(j=0;j<NUM_RAM;j=j+1)
	begin: MEM

		ram_sdp
		#(
			.DATA_WIDTH(30), 
			.ADDR_WIDTH(9)
		)
		ram_inst
		(
			.clk(clk),
			.we(stage_3_wire[k][50*j] & stage_3_wire[k][50*j+1]), 
			.read_addr(stage_3_wire[k][50*j+10 -: 9]),
			.write_addr(stage_3_wire[k][50*j+19 -: 9]),
			.data(stage_3_wire[k][50*j+49 -: 30]),
			.q(stage_3_wire[k+1][50*(j+1)-1 -: 50])
		);
		
	end
end
endgenerate


//-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#
// STAGE 4: DSPS
//-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#

reg [NOC_WIDTH-1:0] stage_4_reg;
reg [NOC_WIDTH-1:0] stage_5_reg;
reg [NOC_WIDTH-1:0] stage_6_reg;
reg [NOC_WIDTH-1:0] stage_7_reg;

always @ (posedge clk)
begin
	if(rst)
		stage_4_reg = 0;
	else
		stage_4_reg = stage_3_wire[NUM_RAM_STAGES];
end

always @ (posedge clk)
begin
	if(rst)
		stage_5_reg = 0;
	else
		stage_5_reg = stage_4_reg;
end

always @ (posedge clk)
begin
	if(rst)
		stage_6_reg = 0;
	else
	begin
		for(i = 0; i < NOC_WIDTH/20; i=i+1)
		begin
			stage_6_reg[NOC_WIDTH-1-i*10   -: 10] = stage_5_reg[NOC_WIDTH/2-1-i*10 -: 10] * stage_5_reg[NOC_WIDTH-1-i*10 -: 10];
			stage_6_reg[NOC_WIDTH/2-1-i*10 -: 10] = stage_5_reg[NOC_WIDTH/2-1-i*10 -: 10] * stage_5_reg[NOC_WIDTH-1-i*10 -: 10];
		end
	end
end


always @ (posedge clk)
begin
	if(rst)
		stage_7_reg = 0;
	else
		stage_7_reg = stage_6_reg;
end

assign outputs = stage_7_reg;

endmodule

/*
 * function : simple dual port RAM module
 * author   : Mohamed S. Abdelfattah
 * date     : 15-JUNE-2015
 */

module ram_sdp
#(parameter DATA_WIDTH=8, parameter ADDR_WIDTH=6)
(
	input [(DATA_WIDTH-1):0] data,
	input [(ADDR_WIDTH-1):0] read_addr, write_addr,
	input we, clk,
	output reg [(DATA_WIDTH+2*ADDR_WIDTH+2-1):0] q
);

	//extra stage of pipeline regs
	reg [(DATA_WIDTH+2*ADDR_WIDTH+2-1):0] q_reg;

	// Declare the RAM variable
	reg [DATA_WIDTH-1:0] ram[2**ADDR_WIDTH-1:0];

	always @ (posedge clk)
	begin
		// Write
		if (we)
			ram[write_addr] <= data;

		// Read (if read_addr == write_addr, return OLD data).	To return
		// NEW data, use = (blocking write) rather than <= (non-blocking write)
		// in the write assignment.	 NOTE: NEW data may require extra bypass
		// logic around the RAM.
		q_reg <= {we,we,read_addr,write_addr,ram[read_addr]}; 
	end
	
	always @ (posedge clk)
		q <= q_reg;

endmodule