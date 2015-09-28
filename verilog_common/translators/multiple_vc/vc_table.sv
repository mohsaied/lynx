/*
 * function : VC table is a shim attached to a sending bundle
 *            it looks up the VC of each destination we're connected to
 * author   : Mohamed S. Abdelfattah
 * date     : 28-SEPT-2015
 */

module vc_table
#(
	parameter N_ADDR_WIDTH = 4,    //router address width
	parameter VC_ADDR_WIDTH = 2,   //VC address width
    parameter NUM_DEST = 4,        //number of destinations
	parameter  [N_ADDR_WIDTH-1:0] DEST [0:NUM_DEST-1] = '{NUM_DEST{1}}, //router index that this vc_table sends to
	parameter [VC_ADDR_WIDTH-1:0]   VC [0:NUM_DEST-1] = '{NUM_DEST{1}}  //VC of each destination
)
(
    input   [N_ADDR_WIDTH-1:0] dest_in,
    output [VC_ADDR_WIDTH-1:0] vc_out
);

//reg for output
reg [VC_ADDR_WIDTH-1:0] vc_out_reg;

//loop variable
integer i;

assign vc_out = vc_out_reg;

//-------------------------------------------------------
// Implementation
//-------------------------------------------------------

always @(*)
begin
    for(i=0;i<NUM_DEST;i++)
    begin
        if(DEST[i]==dest_in)
            vc_out_reg = VC[i];
    end
end

endmodule















