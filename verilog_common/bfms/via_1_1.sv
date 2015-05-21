/*
 * function : Basic dependency point in a simulation model
 * author   : Mohamed S. Abdelfattah
 * date     : 19-MAY-2015
 */

module via_1_1
#(
	parameter i0_WIDTH = 32,                  //data width for i0
	parameter o0_WIDTH = 32,                  //data width for o0
    parameter N = 16,                         //number of nodes
	parameter N_ADDR_WIDTH = $clog2(N),       //router address width
    parameter [7:0] o0_ID = 0,                //unique id associated with each src
    parameter [7:0] i0_ID = 0,                //unique id associated with each sink
	parameter [N_ADDR_WIDTH-1:0] NODE = 15,   //router index that this tpg is connected to
	parameter [N_ADDR_WIDTH-1:0] o0_DEST = 15 //router index that this tpg sends to
)
(
	input clk,
	input rst,
    
    input [i0_WIDTH-1:0] i0_data_in,
    input                i0_valid_in,
    output               i0_ready_out,
    
    output     [o0_WIDTH-1:0] o0_data_out,
    output [N_ADDR_WIDTH-1:0] o0_dest_out,
    output                    o0_valid_out,
    input                     o0_ready_in
);

//control and data positions at each input
//i0 params
localparam i0_SRC_POS  = i0_WIDTH-1;
localparam i0_DST_POS  = i0_SRC_POS - N_ADDR_WIDTH;
localparam i0_ID_POS   = i0_DST_POS - N_ADDR_WIDTH;
localparam i0_DATA_POS = i0_ID_POS - 8;

//registers for ora inputs
//i0 regs
reg                [N_ADDR_WIDTH:0] i0_src_in;
reg                [N_ADDR_WIDTH:0] i0_dst_in;
reg                           [7:0] i0_id_in;
reg [i0_WIDTH-N_ADDR_WIDTH*2-8-1:0] i0_data_counter;
reg                                 i0_buffered_data;
reg                                 i0_ready_reg;

//wires from registers to outputs
//i0 assigns
assign i0_ready_out = i0_ready_reg;

//registers for tpg outputs
//o0 regs
reg [o0_WIDTH-N_ADDR_WIDTH*2-8-1:0] o0_data_counter;
reg              [N_ADDR_WIDTH-1:0] o0_dest_reg;
reg                                 o0_valid_reg;

//wires from registers to outputs
//o0 assigns
assign o0_data_out  = {NODE,o0_dest_reg,o0_ID,o0_data_counter};
assign o0_dest_out  = o0_dest_reg;
assign o0_valid_out = o0_valid_reg;

//dependencies
reg all_inputs_buffered;
reg all_buffered_data_consumed;
reg i0_input_buffered;
reg o0_buffered_data_consumed;

always @ (*)
begin
    all_inputs_buffered = i0_input_buffered;
    all_buffered_data_consumed = o0_buffered_data_consumed;
end

//-------------------------------------------------------
// Implementation
//-------------------------------------------------------

//synopsys translate off
int curr_time;
integer fmain;
initial fmain = $fopen("reports/lynx_trace.txt");
//synopsys translate on

//TPG o0
always @ (posedge clk)
begin
	if (rst)
	begin
        o0_data_counter = 0;
        o0_valid_reg    = 0;
        o0_dest_reg     = 0;
        o0_buffered_data_consumed = 0;
	end
	else
	begin
        if(o0_ready_in && all_inputs_buffered)
        begin
            o0_data_counter = o0_data_counter + 1;
            o0_valid_reg    = 1;
            o0_dest_reg     = o0_DEST;
            
            //synopsys translate off
	        curr_time = $time;
            $fdisplay(fmain,"SRC=%d; time=%d; from=%d; to=%d; id=%d; data=%d;",o0_ID,curr_time,NODE,o0_dest_reg,o0_data_counter);
            $display("SRC=%d; time=%d; from=%d; to=%d; id=%d; data=%d;",o0_ID,curr_time,NODE,o0_dest_reg,o0_data_counter);
            //synopsys translate on
            
            o0_buffered_data_consumed = 1;
        end        
        else
        begin
            o0_valid_reg = 0;
        end
	end
end

//ORA i0
always @ (posedge clk)
begin
	if (rst)
	begin
        i0_ready_reg = 0;
        i0_input_buffered = 1;
	end
	else
	begin
        
        if(all_buffered_data_consumed)
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
                i0_dst_in = i0_data_in[i0_DST_POS -: N_ADDR_WIDTH];
                i0_id_in  = i0_data_in[i0_ID_POS  -: 8];
                i0_data_counter = i0_data_in[i0_DATA_POS : 0];
                
                //synopsys translate off
                curr_time = $time;
                $fdisplay(fmain,"SINK=%d; time=%d; from=%d; to=%d; curr=%d; data=%d; SRC=%d;",i0_ID,curr_time,i0_src_in,i0_dst_in,NODE,i0_data_counter,i0_id_in);
                $display("SINK=%d; time=%d; from=%d; to=%d; curr=%d; data=%d; SRC=%d;",i0_ID,curr_time,i0_src_in,i0_dst_in,NODE,i0_data_counter,i0_id_in);
                //synopsys translate on
                
                i0_input_buffered = 1;
            end    
        end
	end
end


//synopsys translate off
final $fclose(fmain);
//synopsys translate on

endmodule















