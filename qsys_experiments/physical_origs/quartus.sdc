create_clock -period 1 -name clk_clk clk_clk
derive_clock_uncertainty
set_false_path -from reset_reset_n -to reset_reset_n
set_false_path -from altera_reset_* 
set_false_path -to altera_reset_* 
set_false_path -from *|alt_rst_sync_uq1|altera_reset_synchronizer_int_chain_out
set_false_path -to *|alt_rst_sync_uq1|altera_reset_synchronizer_int_chain_out