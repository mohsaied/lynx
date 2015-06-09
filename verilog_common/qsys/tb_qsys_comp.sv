/*
 * function : TB to test straight-through connection of master and slave
 * author   : Mohamed S. Abdelfattah
 * date     : 9-JUNE-2015
 */
 
`timescale 1ns/1ps
module tb_qsys();

parameter           WIDTH = 32;
parameter [7:0]        ID =  0;
parameter [7:0]    DST_ID =  1;
parameter      ADDR_WIDTH = 32;


logic clk;
logic rst;

logic                  mdone;
logic      [WIDTH-1:0] writedata;
logic [ADDR_WIDTH-1:0] address;
logic                  write;
logic                  read;
logic      [WIDTH-1:0] readdata;
logic                  readdatavalid;
logic                  waitrequest;

assign waitrequest = 0;

//clocking
initial clk = 1'b1;
always #5 clk = ~clk; 

//reset 
initial begin
    rst = 1'b1;
    #25;
    rst = 1'b0;
end

qsys_master 
#(
	.WIDTH(WIDTH), 
    .ID(ID), 
    .DST_ID(DST_ID), 
    .ADDR_WIDTH(ADDR_WIDTH)  
)
master_inst
(
    .clk(clk),
	.rst(rst),
    .done(mdone),
    
    .writedata(writedata),
    .address(address),
    .write(write),
    .read(read),
    .readdata(readdata),
    .readdatavalid(readdatavalid),
    .waitrequest(waitrequest)
);

qsys_slave
#(
	.WIDTH(WIDTH), 
    .ID(DST_ID), 
    .ADDR_WIDTH(ADDR_WIDTH)  
)
slave_inst
(
    .clk(clk),
	.rst(rst),
    .done(sdone),
    
    .writedata(writedata),
    .address(address),
    .write(write),
    .read(read),
    .readdata(readdata),
    .readdatavalid(readdatavalid)
);

qsys_halt_sim halter
(
	.done(mdone & sdone)
);

endmodule















