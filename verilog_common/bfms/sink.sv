/*
 * function : Basic traffic analyzer with debug print 
 * author   : Mohamed S. Abdelfattah
 * date     : 19-MAY-2015
 */

module sink
#(
	parameter WIDTH = 32,                    //data width
    parameter N     = 16,                    //number of nodes
	parameter N_ADDR_WIDTH = $clog2(N),      //router address width
    parameter [N_ADDR_WIDTH-1:0] NODE = 15   //router index that this tpg is connected to
)
(
	input clk,
	input rst,
    
    input [WIDTH-1:0] data_in,
    input             valid_in,
    output            ready_out
);

localparam SRC_POS  = WIDTH-1;
localparam DST_POS  = SRC_POS - N_ADDR_WIDTH;
localparam ID_POS   = DST_POS - N_ADDR_WIDTH;
localparam DATA_POS = ID_POS - 8;

//components of data_in
reg [N_ADDR_WIDTH:0] src_in;
reg [N_ADDR_WIDTH:0] dst_in;
reg [7:0] id_in;
reg [WIDTH-N_ADDR_WIDTH*2-8-1:0] data_counter;

reg ready_reg;

//we're always ready - for now
assign ready_out = ready_reg;

//-------------------------------------------------------
// Implementation
//-------------------------------------------------------

//synopsys translate off
int curr_time;
integer fmain;
initial fmain = $fopen("lynx_trace.txt");
//synopsys translate on

//recieve data
always @ (posedge clk)
begin
	if (rst)
	begin
        ready_reg = 0;
	end
	else
	begin
        ready_reg = 1;
        if(valid_in)
        begin
            src_in = data_in[SRC_POS -: N_ADDR_WIDTH];
            dst_in = data_in[DST_POS -: N_ADDR_WIDTH];
            id_in  = data_in[ID_POS  -: 8];
            data_counter = data_in[DATA_POS : 0];
            
            //synopsys translate off
	        curr_time = $time;
            $fdisplay(fmain,"RECV; time=%d; from=%d; to=%d; curr=%d; id=%d; data=%d;",curr_time,src_in,dst_in,NODE,id_in,data_counter);
            $display("RECV; time=%d; from=%d; to=%d; curr=%d; id=%d; data=%d;",curr_time,src_in,dst_in,NODE,id_in,data_counter);
            //synopsys translate on
        end        
	end
end

//synopsys translate off
final $fclose(fmain);
//synopsys translate on

endmodule















