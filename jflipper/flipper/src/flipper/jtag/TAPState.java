package flipper.jtag;

public enum TAPState {
	TestLogicReset,
	RunTestIdle,
	
	SelectDRScan,
	CaptureDR,
	ShiftDR,
	Exit1DR,
	PauseDR,
	Exit2DR,
	UpdateDR,
	
	SelectIRScan,
	CaptureIR,
	ShiftIR,
	Exit1IR,
	PauseIR,
	Exit2IR,
	UpdateIR,
	
	Unknown;
	
	public static final int NumStates = 16;
}
