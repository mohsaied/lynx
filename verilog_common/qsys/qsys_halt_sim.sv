/*
 * function : halt the simulation when told
 * author   : Mohamed S. Abdelfattah
 * date     : 9-JUNE-2015
 */

module qsys_halt_sim
(
    input done
);

//synopsys translate off
always @ (*)
    if(done)
    begin
        $display("Shutting down simulation, good bye, thank you for playing with the lynx simulator!");
        $finish(0);
    end
//synopsys translate on

endmodule















