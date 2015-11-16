/*
 * function : Qsys slave with debug messages for perf eval
 * author   : Mohamed S. Abdelfattah
 * date     : 9-JUNE-2015
 */

module qsys_master
#(
	parameter           WIDTH     = 32, //data width
    parameter [7:0]    SRC_ID     =  2, //unique id associated with each src
    parameter [7:0]    SNK_ID     =  3, //unique id associated with each sink
    parameter [7:0]    DST_ID     =  1, //id associated with the destination
    parameter [7:0]    NUM_SLAVES =  0, //Number of slaves we're sending to 
    parameter [7:0]    BURST_SIZE =  1, //burst length
    parameter          ADDR_WIDTH = 32  //address width doesn't matter
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

reg [7:0] burst_counter;

//do we have something queued?
reg queued_flag;
reg read_reg;

reg [ADDR_WIDTH-1:0] address_reg;

assign writedata = {SRC_ID,DST_ID,data_counter};
assign address   = address_reg;
assign write     = 0;
assign read      = read_reg;

//parse out things from incoming response
localparam SRC_POS  = WIDTH-1;
localparam DST_POS  = SRC_POS - 8;
localparam DATA_POS = DST_POS - 8;

localparam ADDR_MOD = ADDR_WIDTH-32;

reg [7:0] src_id_in;
reg [7:0] dst_id_in;
reg [WIDTH-8*2-1:0] data_in;

reg [7:0] curr_dst;

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
        read_reg = 0;
        address_reg = 0;
        curr_dst = 0;
        burst_counter = BURST_SIZE;
	end
	else
	begin
        if(!waitrequest)
        begin
            data_counter = data_counter + 1;
            queued_flag  = 0;
            read_reg = 1;

            //select which slave to send to next
            //slaves always have a 32-bit address
            address_reg[ADDR_WIDTH-1 -: ADDR_MOD] = curr_dst;
            
            burst_counter = burst_counter - 1;
            
            if(burst_counter === 0)
                curr_dst = curr_dst + 1;
            if(curr_dst === NUM_SLAVES)
                curr_dst = 0;
                
            if(burst_counter === 0)
                burst_counter = BURST_SIZE;
            
            //synopsys translate off
	        curr_time = $time;
            $fdisplay(fmain,"SRC=%d; time=%d; from=0; to=0; curr=0; data=%d;",SRC_ID,curr_time,data_counter);
            $display("SRC=%d; time=%d; from=0; to=0; curr=0; data=%d;",SRC_ID,curr_time,data_counter);
            //synopsys translate on
        end        
        else
        begin
            //synopsys translate off
            if(queued_flag == 0) //we only want to print the queued message once
            begin
                curr_time = $time;
                $fdisplay(fmain,"SRC=%d; time=%d; from=0; to=0; curr=0; data=%d; QUEUED=1;",SRC_ID,curr_time,data_counter+1);
                $display("SRC=%d; time=%d; from=0; to=0; curr=0; data=%d; QUEUED=1;",SRC_ID,curr_time,data_counter+1);
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
        $fdisplay(fmain,"SINK=%d; time=%d; from=0; to=0; curr=0; data=%d; SRC=%d;",SNK_ID,curr_time,data_in,src_id_in);
        $display("SINK=%d; time=%d; from=0; to=0; curr=0; data=%d; SRC=%d;",SNK_ID,curr_time,data_in,src_id_in);
        //synopsys translate on 
    end
end

//time bomb to end simulation after 100 pieces of data
assign done = data_counter > 1000;


//synopsys translate off
final $fclose(fmain);
//synopsys translate on

endmodule















