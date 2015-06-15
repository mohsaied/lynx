/*
 * function : Qsys slave with debug messages for perf eval
 * author   : Mohamed S. Abdelfattah
 * date     : 9-JUNE-2015
 */

module qsys_slave
#(
	parameter            WIDTH = 32, //data width
    parameter [7:0]     SRC_ID = 0,  //unique id associated with each src
    parameter [7:0]     SNK_ID = 1,  //unique id associated with each src
    parameter       ADDR_WIDTH = 30  //address width doesn't matter
)
(
	input clk,
	input rst,
    output done,
    
    input  wire      [WIDTH-1:0] writedata,
    input  wire [ADDR_WIDTH-1:0] address,
    input  wire                  write,
    input  wire                  read,
    output wire      [WIDTH-1:0] readdata,
    output wire                  readdatavalid
);

//counter for data at this node
reg [WIDTH-8*2-1:0] data_counter;
reg readdatavalid_reg;

//do we have something queued?
reg request;

//parse out things from incoming response
localparam SRC_POS  = WIDTH-1;
localparam DST_POS  = SRC_POS - 8;
localparam DATA_POS = DST_POS - 8;

reg [7:0] src_id_in;
reg [7:0] dst_id_in;
reg [WIDTH-8*2-1:0] data_in;

assign readdata = {SRC_ID,src_id_in,data_counter};
assign readdatavalid = readdatavalid_reg;

//-------------------------------------------------------
// Implementation
//-------------------------------------------------------

//we always want to issue read requests so that we get replies back
//but we won't necessarily wait for the replies - we'll keep sending read requests

//synopsys translate off
int curr_time;
integer fmain;
initial fmain = $fopen("reports/qsys_trace.txt");
//synopsys translate on

//send data whenever we get a request
always @ (posedge clk)
begin
	if (rst)
	begin
        data_counter = 0;
        readdatavalid_reg = 0;
	end
	else
	begin
        if(request)
        begin
            readdatavalid_reg = 1;
            
            //synopsys translate off
	        curr_time = $time;
            $fdisplay(fmain,"SRC=%d; time=%d; from=0; to=0; curr=0; data=%d;",SRC_ID,curr_time,data_counter);
            $display("SRC=%d; time=%d; from=0; to=0; curr=0; data=%d;",SRC_ID,curr_time,data_counter);
            //synopsys translate on
            
            data_counter = data_counter + 1;
        end
        else
        begin
            readdatavalid_reg = 0;
        end
	end
end

//read data whenever valid
always @ (posedge clk)
begin
    if(~rst)
    if(read | write)
    begin
        request = 1;
        src_id_in = writedata[SRC_POS -: 8];
        dst_id_in = writedata[DST_POS -: 8];
        data_in   = writedata[DATA_POS : 0];
        
        //synopsys translate off
        curr_time = $time;
        $fdisplay(fmain,"SINK=%d; time=%d; from=0; to=0; curr=0; data=%d; SRC=%d;",SNK_ID,curr_time,data_in,src_id_in);
        $display("SINK=%d; time=%d; from=0; to=0; curr=0; data=%d; SRC=%d;",SNK_ID,curr_time,data_in,src_id_in);
        //synopsys translate on 
    end
    else
    begin
        request = 0;
    end
end

//time bomb to end simulation after 100 pieces of data
assign done = data_counter > 1000;


//synopsys translate off
final $fclose(fmain);
//synopsys translate on

endmodule















