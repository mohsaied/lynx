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
    parameter NUM_DEST = 4,                  //number of destinations for output 0
	parameter [N_ADDR_WIDTH-1:0] DEST [0:NUM_DEST-1] = '{NUM_DEST{1}} //router index that this tpg sends to
)
(
	input clk,
	input rst,
    output done,
    
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

//count the dst we're sending to
integer dstcount;

assign data_out  = {NODE,dest_reg,ID,data_counter};
assign dest_out  = dest_reg;
assign valid_out = valid_reg;

//-------------------------------------------------------
// Implementation
//-------------------------------------------------------

//synopsys translate off
int curr_time;
integer fmain;
initial fmain = $fopen("reports/lynx_trace.txt");
//synopsys translate on

//send data whenever possible
always @ (posedge clk)
begin
	if (rst)
	begin
        data_counter = 0;
        valid_reg    = 0;
        dest_reg     = 0;
        dstcount     = 0;
	end
	else
	begin
        if(ready_in)
        begin
            data_counter = data_counter + 1;
            valid_reg = 1;
            
            dest_reg = DEST[dstcount];
            
            dstcount = dstcount + 1;
            if(dstcount == NUM_DEST)
                dstcount = 0;
            
            
            //synopsys translate off
	        curr_time = $time;
            $fdisplay(fmain,"SRC=%d;  time=%d; from=%d; to=%d; curr=%d; data=%d;",ID,curr_time,NODE,dest_reg,NODE,data_counter);
            $display("SRC=%d;  time=%d; from=%d; to=%d; curr=%d; data=%d;",ID,curr_time,NODE,dest_reg,NODE,data_counter);
            //synopsys translate on
        end        
        else
        begin
            valid_reg    = 0;            
        end
	end
end

//time bomb to end simulation after 100 pieces of data
assign done = data_counter > 1000;


//synopsys translate off
final $fclose(fmain);
//synopsys translate on

endmodule















