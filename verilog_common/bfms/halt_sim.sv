/*
 * function : halt the simulation when told
 * author   : Mohamed S. Abdelfattah
 * date     : 21-MAY-2015
 */

module halt_sim
(
    input done
);

//synopsys translate off
always @ (*)
    if(done)
        $finish(0);
//synopsys translate on

endmodule















