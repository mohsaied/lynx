/*
 * function : Basic dependency point in a simulation model 
 * - returns data to whoever sent to it in nodep=0 mode
 * - and returns data to senders in the DEST field otherwise
 * author   : Mohamed S. Abdelfattah
 * date     : 9-JUNE-2015
 */

//naming: via_numsrc_numsink
module via_1_1
#(
parameter             N = 16,               //number of nodes
parameter        NUM_VC = 2,                //number of VCs
parameter  N_ADDR_WIDTH = $clog2(N),        //router address width
parameter VC_ADDR_WIDTH = $clog2(NUM_VC),   //router address width

parameter o0_WIDTH = 32,                    //data width for o0
parameter i0_WIDTH = 32,                    //data width for i0

parameter [7:0] o0_ID = 0,                  //unique id associated with each src
parameter [7:0] i0_ID = 0,                  //unique id associated with each sink

parameter  [N_ADDR_WIDTH-1:0] o0_NODE = 15, //router index that this tpg is connected to
parameter  [N_ADDR_WIDTH-1:0] i0_NODE = 15, //router index that this ora is connected to
parameter [VC_ADDR_WIDTH-1:0] i0_VC   = 0,  //vc index that this ora is connected to

parameter o0_NODEP = 1'b0,                  // NODEP = 1 indicates that this via won't wait for a response to send a reply -- it'll just keep sending when it can
parameter RETURN_TO_SENDER = !o0_NODEP,     // if there is a dependency, then this via replies to the same module that sent stuff to it

parameter o0_NUM_DEST = 4,  //number of destinations for output 0
parameter  [N_ADDR_WIDTH-1:0] o0_DEST [0:o0_NUM_DEST-1] = '{o0_NUM_DEST{1}}, //router index that this tpg sends to
parameter [VC_ADDR_WIDTH-1:0]   o0_VC [0:o0_NUM_DEST-1] = '{o0_NUM_DEST{1}}, //vc index that this tpg sends to
parameter NUM_TESTS = 1000 // will stop the simulation aafter this many pieces of data are sent and recieved
)
(
	input clk,
	input rst,
	output done,
    
    input [i0_WIDTH-1:0] i0_data_in,
    input                i0_valid_in,
    output               i0_ready_out,
    
    output      [o0_WIDTH-1:0] o0_data_out,
    output  [N_ADDR_WIDTH-1:0] o0_dest_out,
    output [VC_ADDR_WIDTH-1:0] o0_vc_out,
    output                     o0_valid_out,
    input                      o0_ready_in
);

//control and data positions at each input
//i0 params
localparam i0_RETURN_POS   = i0_WIDTH-1;
localparam i0_RETURNVC_POS = i0_RETURN_POS - N_ADDR_WIDTH;
localparam i0_SRC_POS      = i0_RETURNVC_POS - VC_ADDR_WIDTH;
localparam i0_DST_POS      = i0_SRC_POS - N_ADDR_WIDTH;
localparam i0_VC_POS       = i0_DST_POS - N_ADDR_WIDTH;
localparam i0_ID_POS       = i0_VC_POS - VC_ADDR_WIDTH;
localparam i0_DATA_POS     = i0_ID_POS - 8;

localparam i0_DATA_COUNTER_WIDTH = i0_WIDTH - N_ADDR_WIDTH*3 - VC_ADDR_WIDTH*2 - 8;
localparam o0_DATA_COUNTER_WIDTH = o0_WIDTH - N_ADDR_WIDTH*3 - VC_ADDR_WIDTH*2 - 8;

//registers for ora inputs
//i0 regs
reg            [N_ADDR_WIDTH-1:0] i0_src_in;
reg            [N_ADDR_WIDTH-1:0] i0_return_in;
reg           [VC_ADDR_WIDTH-1:0] i0_returnvc_in;
reg            [N_ADDR_WIDTH-1:0] i0_dst_in;
reg                         [7:0] i0_id_in;
reg [i0_DATA_COUNTER_WIDTH - 1:0] i0_data_counter;
reg                               i0_buffered_data;
reg                               i0_ready_reg;

//wires from registers to outputs
//i0 assigns
assign i0_ready_out = i0_ready_reg;

//registers for tpg outputs
//o0 regs
//update the data counter width if I add any control fields in the data being sent
reg [o0_DATA_COUNTER_WIDTH - 1:0] o0_data_counter;
reg            [N_ADDR_WIDTH-1:0] o0_dest_reg;
reg           [VC_ADDR_WIDTH-1:0] o0_vc_reg;
reg                               o0_valid_reg;

//flag indicating that something is queued
reg o0_queued_flag;

//count the dst we're sending to
integer o0_dstcount;

//wires from registers to outputs
//o0 assigns
assign o0_data_out  = {i0_NODE, i0_VC, o0_NODE, o0_dest_reg, o0_vc_reg, o0_ID, o0_data_counter};

assign o0_dest_out  = o0_dest_reg;
assign o0_vc_out    = o0_vc_reg;
assign o0_valid_out = o0_valid_reg;

//dependencies
reg all_inputs_buffered;
reg all_buffered_data_consumed;
reg i0_input_buffered;
reg o0_buffered_data_consumed;
reg all_outputs_ready;

always @ (*)
begin
    all_inputs_buffered = i0_input_buffered;
    all_buffered_data_consumed = o0_buffered_data_consumed;
    all_outputs_ready = o0_ready_in;
end

//-------------------------------------------------------
// Implementation
//-------------------------------------------------------

//synopsys translate off
int curr_time;
integer fmain;
integer fextra;
initial fmain = $fopen("reports/lynx_trace.txt");
initial fextra = $fopen("reports/lynx_trace_extra.txt");
//synopsys translate on

//TPG o0
always @ (posedge clk)
begin
	if (rst)
	begin
        o0_data_counter = 0;
        o0_valid_reg    = 0;
        o0_dest_reg     = 0;
        o0_vc_reg       = 0;
        o0_dstcount     = 0;
        o0_buffered_data_consumed = 0;
        o0_queued_flag = 0;
	end
	else
	begin
        if(o0_ready_in && (all_inputs_buffered || o0_NODEP))
        begin
            o0_data_counter = o0_data_counter + 1;
            o0_valid_reg    = 1;
            o0_queued_flag = 0;
            
            if(!RETURN_TO_SENDER) begin
                o0_dest_reg = o0_DEST[o0_dstcount];
                o0_vc_reg = o0_VC[o0_dstcount];
            end else begin
                o0_dest_reg = i0_return_in;
                o0_vc_reg = i0_returnvc_in;
            end
            
            o0_dstcount = o0_dstcount + 1;
            if(o0_dstcount == o0_NUM_DEST)
                o0_dstcount = 0;
            
            //synopsys translate off
	        curr_time = $time;
            $fdisplay(fmain,"SRC=%d;  time=%d; from=%d; to=%d; curr=%d; data=%d;",o0_ID,curr_time,o0_NODE,o0_dest_reg,o0_NODE,o0_data_counter);
            $fdisplay(fextra,"SRC=%d;  time=%d; from=%d; fromvc=%d; to=%d; tovc=%d; curr=%d; data=%d;",o0_ID,curr_time,o0_NODE,i0_VC,o0_dest_reg,o0_vc_reg,o0_NODE,o0_data_counter);
            //$display("SRC=%d;  time=%d; from=%d; to=%d; curr=%d; data=%d;",o0_ID,curr_time,o0_NODE,o0_dest_reg,o0_NODE,o0_data_counter);
            //synopsys translate on
            
            o0_buffered_data_consumed = 1;
        end        
        else
        begin
            o0_buffered_data_consumed = o0_NODEP;
            o0_valid_reg = 0;
            
            //synopsys translate off
            if(o0_queued_flag == 0) //we only want to print the queued message once
            begin
                curr_time = $time;
                $fdisplay(fmain,"SRC=%d;  time=%d; from=%d; to=%d; curr=%d; data=%d;QUEUED=1;",o0_ID,curr_time,o0_NODE,o0_dest_reg,o0_NODE,o0_data_counter+1);
                //$display("SRC=%d;  time=%d; from=%d; to=%d; curr=%d; data=%d;QUEUED=1;",o0_ID,curr_time,o0_NODE,o0_dest_reg,o0_NODE,o0_data_counter+1);
            end
            //synopsys translate on
            
            o0_queued_flag = 1;
        end
	end
end

//ORA i0
always @ (posedge clk)
begin
	if (rst)
	begin
        i0_ready_reg = 0;
        i0_input_buffered = 0;
	end
	else
	begin
        
        if(all_buffered_data_consumed || all_outputs_ready)
        begin
            i0_input_buffered = 0;
        end
        
        if(i0_input_buffered)
        begin
            i0_ready_reg = 0;
        end
        else
        begin
            i0_ready_reg = 1;
            if(i0_valid_in)
            begin
                i0_src_in = i0_data_in[i0_SRC_POS -: N_ADDR_WIDTH];
                i0_return_in = i0_data_in[i0_RETURN_POS -: N_ADDR_WIDTH];
                i0_returnvc_in = i0_data_in[i0_RETURNVC_POS -: VC_ADDR_WIDTH];
                i0_dst_in = i0_data_in[i0_DST_POS -: N_ADDR_WIDTH];
                i0_id_in  = i0_data_in[i0_ID_POS  -: 8];
                i0_data_counter = i0_data_in[i0_DATA_POS : 0];
                
                //synopsys translate off
                curr_time = $time;
                $fdisplay(fmain,"SINK=%d; time=%d; from=%d; to=%d; curr=%d; data=%d; SRC=%d;",i0_ID,curr_time,i0_src_in,i0_dst_in,i0_NODE,i0_data_counter,i0_id_in);
                $fdisplay(fextra,"SINK=%d; time=%d; from=%d; fromvc=%d; to=%d; tovc=%d; curr=%d; data=%d; SRC=%d;",i0_ID,curr_time,i0_src_in,i0_returnvc_in,i0_dst_in,i0_VC,i0_NODE,i0_data_counter,i0_id_in);
                //$display("SINK=%d; time=%d; from=%d; to=%d; curr=%d; data=%d; SRC=%d;",i0_ID,curr_time,i0_src_in,i0_dst_in,i0_NODE,i0_data_counter,i0_id_in);
                //synopsys translate on
                
                i0_input_buffered = 1;
            end    
        end
	end
end


//time bomb to end simulation after NUM_TESTS pieces of data
assign done = (i0_data_counter > NUM_TESTS) && (o0_data_counter > NUM_TESTS);

//synopsys translate off
final $fclose(fmain);
final $fclose(fextra);
//synopsys translate on

endmodule















