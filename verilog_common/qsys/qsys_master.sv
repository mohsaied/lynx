/*
 * function : Qsys slave with debug messages for perf eval
 * author   : Mohamed S. Abdelfattah
 * date     : 9-JUNE-2015
 */

module qsys_master
#(
	parameter           WIDTH = 32, //data width
    parameter [7:0]        ID =  0, //unique id associated with each src
    parameter [7:0]    DST_ID =  0, //id associated with the destination
    parameter      ADDR_WIDTH = 32  //address width doesn't matter
)
(
	input clk,
	input rst,
    output done,
    
    output wire      [WIDTH-1:0] writedata,
    output wire [ADDR_WIDTH-1:0] address,
    output wire                  write,
    output wire                  read,
    input  wire      [WIDTH-1:0] readdata,
    input  wire                  readdatavalid,
    input  wire                  waitrequest
);

//counter for data at this node
reg [WIDTH-8*2-1:0] data_counter;

//do we have something queued?
reg queued_flag;

assign writedata = {ID,DST_ID,data_counter};
assign address   = 0;
assign write     = 0;
assign read      = 1;

//parse out things from incoming response
localparam SRC_POS  = WIDTH-1;
localparam DST_POS  = SRC_POS - 8;
localparam DATA_POS = DST_POS - 8;

reg [7:0] src_id_in;
reg [7:0] dst_id_in;
reg [WIDTH-8*2-1:0] data_in;

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

//send data whenever possible
always @ (posedge clk)
begin
	if (rst)
	begin
        data_counter = 0;
        queued_flag  = 0;
	end
	else
	begin
        if(!waitrequest)
        begin
            data_counter = data_counter + 1;
            queued_flag  = 0;
            
            //synopsys translate off
	        curr_time = $time;
            $fdisplay(fmain,"SRC=%d; DST=%d; time=%d; data=%d;",ID,DST_ID,curr_time,data_counter);
            $display("SRC=%d; DST=%d; time=%d; data=%d;",ID,DST_ID,curr_time,data_counter);
            //synopsys translate on
        end        
        else
        begin
            
            //synopsys translate off
            if(queued_flag == 0) //we only want to print the queued message once
            begin
                curr_time = $time;
                $fdisplay(fmain,"SRC=%d; DST=%d; time=%d; data=%d; MASTER; QUEUED=1;",ID,DST_ID,curr_time,data_counter+1);
                $display("SRC=%d; DST=%d; time=%d; data=%d; MASTER; QUEUED=1;",ID,DST_ID,curr_time,data_counter+1);
            end
            //synopsys translate on  
            
            queued_flag  = 1;      
        end
	end
end

//read data whenever valid
always @ (posedge clk)
begin
    if(readdatavalid)
    begin
        src_id_in = readdata[SRC_POS -: 8];
        dst_id_in = readdata[DST_POS -: 8];
        data_in   = readdata[DATA_POS : 0];
        
        //synopsys translate off
        curr_time = $time;
        $fdisplay(fmain,"SINK=%d; SRC=%d; time=%d; data=%d; MASTER;",src_id_in,dst_id_in,curr_time,data_in);
        $display("SINK=%d; SRC=%d; time=%d; data=%d; MASTER;",src_id_in,dst_id_in,curr_time,data_in);
        //synopsys translate on 
    end
end

//time bomb to end simulation after 100 pieces of data
assign done = data_counter > 1000;


//synopsys translate off
final $fclose(fmain);
//synopsys translate on

endmodule















