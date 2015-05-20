/*
 * function : Basic traffic generator with debug print 
 * author   : Mohamed S. Abdelfattah
 * date     : 19-MAY-2015
 */

module src
#(
	parameter WIDTH = 32,                    //data width
    parameter N     = 16,                    //number of nodes
	parameter N_ADDR_WIDTH = $clog2(N),      //router address width
    parameter [7:0] ID = 0,                  //unique id associated with each src
	parameter [N_ADDR_WIDTH-1:0] NODE = 15,  //router index that this src is connected to
	parameter [N_ADDR_WIDTH-1:0] DEST = 15   //router index that this src sends to
)
(
	input clk,
	input rst,
    
    output        [WIDTH-1:0] data_out,
    output [N_ADDR_WIDTH-1:0] dest_out,
    output                    valid_out,
    input                     ready_in
);

//counter for data at this node
reg [WIDTH-N_ADDR_WIDTH*2-8-1:0] data_counter;

//registers for outputs
reg [N_ADDR_WIDTH-1:0] dest_reg;
reg                    valid_reg;

assign data_out  = {NODE,dest_reg,ID,data_counter};
assign dest_out  = dest_reg;
assign valid_out = valid_reg;

//-------------------------------------------------------
// Implementation
//-------------------------------------------------------

//synopsys translate off
int curr_time;
integer fmain;
initial fmain = $fopen("lynx_trace.txt");
//synopsys translate on

//send data whenever possible
always @ (posedge clk)
begin
	if (rst)
	begin
        data_counter = 0;
        valid_reg    = 0;
        dest_reg     = 0;
	end
	else
	begin
        if(ready_in)
        begin
            data_counter = data_counter + 1;
            valid_reg    = 1;
            dest_reg     = DEST;
            
            //synopsys translate off
	        curr_time = $time;
            $fdisplay(fmain,"SEND; time=%d; from=%d; to=%d; curr=%d; id=%d; data=%d;",curr_time,NODE,dest_reg,NODE,ID,data_counter);
            $display("SEND; time=%d; from=%d; to=%d; curr=%d; id=%d; data=%d;",curr_time,NODE,dest_reg,NODE,ID,data_counter);
            //synopsys translate on
        end        
        else
        begin
            data_counter = data_counter;
            valid_reg    = 0;
            dest_reg     = DEST;            
        end
	end
end

//time bomb to end simulation after 100 pieces of data
//synopsys translate off
always @ (posedge clk)
if(data_counter == 100)
    $finish(0);
//synopsys translate on


//synopsys translate off
final $fclose(fmain);
//synopsys translate on

endmodule















