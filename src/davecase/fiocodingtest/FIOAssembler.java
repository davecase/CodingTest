/**
 * 
 */
package davecase.fiocodingtest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * @author Dave Case
 *
 */
public class FIOAssembler {
	private static final int ERROR_RESULT = 1;
	private static final int NO_ERRORS = 0;
	private static final int MAX_12BIT_ADDRESS = new Double(Math.pow(2, 12)).intValue() - 1;
	private BufferedReader input = null;
	private int startingAddress = 0;
	private List<String> machineInstructions = new ArrayList<String>();
	private Set<String> hardwareInstructions = new HashSet<String>();
	private Map<String, String> opcodes = new HashMap<String, String>();
	private String register = null;

	/**
	 * Constructor telling program where to read program
	 * @param in 
	 */
	public FIOAssembler(InputStream in) {
		input = new BufferedReader(new InputStreamReader(in));
	}

	/**
	 * Main method - for starting from command prompt or shell.
	 * @param args Optional args from the specification - ignored
	 */
	public static void main(String[] args) {
		FIOAssembler program = new FIOAssembler(System.in);
		program.run();
		System.exit(NO_ERRORS);
	}

	/**
	 * Method to process to the input file and produce the output
	 */
	private void run() {
		String line = null;
		buildHardwareInstructions();
		buildOpCodes();
		line = readLine();
		if (line == null) {
			exitBecauseOfError("Input Stream EOF encountered before processing any input");
		}
		if (line.trim().startsWith("*")) {
			handleStartingAddress(line);
			line = readLine();
		}
		while (line != null) {
			processLine(line);
			line = readLine();
		}
		reportMachineCode();
	}

	/**
	 * Read and assembler directive and process appropriately
	 * @param line Input line containing assembler directive
	 */
	private void handleStartingAddress(String line) {
		String trimmed = line.trim();
		if (trimmed.length() < 2) {
			return; // no operand for starting address, assume 0 which is default value
		}
		String octal = trimmed.substring(1).trim();
		startingAddress = convertToDecimal(octal);
	}

	/**
	 * Read an octal string, convert to decimal, and make sure it fits within the address specifications.
	 * @param octal The octal string to be converted
	 * @return Returns the decimal value of the input octal string
	 */
	private int convertToDecimal(String octal) {
		int result = 0;
		try {
			result = Integer.parseInt(octal, 8);
		} catch (NumberFormatException e) {
			exitBecauseOfError("Invalid octal value, scanning :" + octal + ":");
		}
		if (result < 0 || result > MAX_12BIT_ADDRESS) {
			exitBecauseOfError("Invalid value to be represented in 12 bits, scanning " + octal);
		}
		return result;
	}

	/**
	 * Process the indicated assembler line 
	 * @param line Line to be processed
	 */
	private void processLine(String line) {
		String trimmed = line.trim();
		if (trimmed.length() < 1) {
			exitBecauseOfError("Empty input line found");
		}
		for (String opcode : hardwareInstructions) {
			if (trimmed.length() >= opcode.length()) {
				if (trimmed.toLowerCase().startsWith(opcode.toLowerCase())) {
					buildInstruction(trimmed, opcode);
					break;
				}
			}
				
		}
	}

	/**
	 * Validate and build the machine instruction according to specified rules
	 * @param trimmed The trimmed line of input
	 * @param opcode The operator found to start the input
	 */
	private void buildInstruction(String trimmed, String opcode) {
		String remainder = trimmed.substring(opcode.length());
		if (remainder.length() > 0 && remainder.startsWith(",")) {
			exitBecauseOfError("RULE VIOLATION: Whitespace is optional between opcode mnemonics and operands and will consist only of space characters, scanning " + trimmed);
		}
		StringTokenizer st = new StringTokenizer(remainder, ", ", false);
		List<String> list = new ArrayList<String>();
		while (st.hasMoreTokens()) {
			list.add(st.nextToken());
		}
		String [] tokens = list.toArray(new String[0]);
		if (tokens.length > 2) {
			exitBecauseOfError("Too many operands, scanning " + remainder);
		}
		switch (opcode.toLowerCase()) {
		case "stop":
			addMachineInstruction(opcode, "");
			break;
		case "twld":
			if(tokens.length != 2) {
				exitBecauseOfError("Missing operand, scanning " + trimmed);
			}
			addMachineInstruction(opcode, tokens[0].trim(), tokens[1].trim());
			break;
		case "twst":
			if(tokens.length != 2) {
				exitBecauseOfError("Missing operand, scanning " + trimmed);
			}
			addMachineInstruction(opcode, tokens[0].trim(), tokens[1].trim());
			break;
		case "and":
			determineRegister(tokens);
			addMachineInstruction(opcode, register);
			break;
		case "clr":
			determineRegister(tokens);
			addMachineInstruction(opcode, register);
			break;
		default:
			exitBecauseOfError("Internal error: invalid op, scanning " + opcode);
			break;
		}
	}

	/**
	 * Set the global register string value to represent the registers being utilized in the opcode.  The string value
	 * in the register string must be in the proper order so that, when combined with the opcode, the machinecode can be
	 * found in the Map.
	 * @param tokens Tokens which represent the list of registers specified by the program.
	 */
	private void determineRegister(String[] tokens) {
		/*
		 * This is probably not what Fusion IO is looking for.
		 * This is hard coded to look for registers j and k.  It probably should have
		 * been done by setting the appropriate bits in the opcode and building
		 * the opcode that way.  But the instructions did not say it had to be done
		 * with bit manipulation, just that the assembler had to be able to support
		 * the registers in either order.
		 */
		String token = tokens[0];
		if (tokens.length == 1) {
			if (token.length() == 1) {
				register = token;
				return;
			}
			if (token.length() > 2) {
				exitBecauseOfError("Error: register specification contains too many registers, scanning " + token);
			}
			List<String> list = new ArrayList<String>();
			for (int i = 0; i < token.length(); i++) {
				list.add(token.substring(i, i+1));
			}
			String[] tokenList = list.toArray(new String[list.size()]);
			determineRegister(tokenList);
			return;
		}
		if (token.equalsIgnoreCase(tokens[1])) {
			exitBecauseOfError("Same register specified multiple times: " + token);
		}
		String result = "";
		if (token.equalsIgnoreCase("j") || tokens[1].equalsIgnoreCase("j")) {
			result += "j";
		}
		if (token.equalsIgnoreCase("k") || tokens[1].equalsIgnoreCase("k")) {
			result += "k";
		}
		if (result.length() != tokens.length) {
			exitBecauseOfError("Invalid register names, scanning " + token + ":" + tokens[1]);
		}
		register = result;
	}

	/**
	 * Add the Machine Instruction to the output.  This invocation of the overloaded method is used 
	 * for the single-word instruction set (i.e. 'stop', 'and', and 'clr') or to load the first
	 * word of the two-word instructions.
	 * @param opcode The opcode being processed
	 * @param register The register specification
	 */
	private void addMachineInstruction(String opcode, String register) {
		String key = (opcode + register).toLowerCase();
		String machineOpcode = opcodes.get(key);
		if (machineOpcode == null) {
			exitBecauseOfError("Unable to determin opcode for " + opcode + ":" + register);
		}
		machineInstructions.add(machineOpcode);
	}

	/**
	 * Add the Machine Instruction to the output.  This invocation of the overloaded method is used
	 * for the double-word instruction set (i.e. 'twld' and 'twst').  It calls the other invocation of the overloaded
	 * method for the first word, and then adds the memory address operand. 
	 * @param opcode  The opcode being processed
	 * @param register The register specification
	 * @param address The memory address operand
	 */
	private void addMachineInstruction(String opcode, String register, String address) {
		addMachineInstruction(opcode, register);
		int addr = convertToDecimal(address);
		String addrStr = Integer.toOctalString(addr);
		while (addrStr.length() < 4) {
			addrStr = "0" + addrStr;
		}
		machineInstructions.add(addrStr);
	}

	/**
	 * Prints out the list of machine instructions.  Each line of output consists of
	 * the memory address being reported as a 12-bit octal string, a space, and then
	 * the opcode or memory address as a 12-bit octal string.
	 */
	private void reportMachineCode() {
		for (int i = 0; i < machineInstructions.size(); i++) {
			Integer addr = i + startingAddress;
			String addrStr = Integer.toOctalString(addr);
			if (addr > MAX_12BIT_ADDRESS) {
				exitBecauseOfError("Invalid address detected, found " + addrStr);
			}
			while (addrStr.length() < 4) {
				addrStr = "0" + addrStr;
			}
			System.out.println(addrStr + " " + machineInstructions.get(i));
		}
	}

	/**
	 * Reads the next line of input from the specified source.
	 * @return A string which represents the next line of input.
	 */
	private String readLine() {
		String result = null;
		try {
			result = input.readLine();
		} catch (IOException e) {
			exitBecauseOfError("IOException encoutered while reading input: " + e.getMessage());
		}
		return result;
	}
	
	/**
	 * Ouput an error message and exit from the program with an error code.
	 * @param errorMessage  The error message to be written to standard error.
	 */
	private void exitBecauseOfError(String errorMessage) {
		System.err.println(errorMessage);
		System.exit(ERROR_RESULT);
	}

	/**
	 * Build the list of valid Hardware Instructions.
	 */
	private void buildHardwareInstructions() {
		hardwareInstructions.add("stop");
		hardwareInstructions.add("twld");
		hardwareInstructions.add("twst");
		hardwareInstructions.add("and");
		hardwareInstructions.add("clr");
	}
	
	/**
	 * Build the map of hardware opcodes and the machine code.  The hardware opcodes
	 * are a combination of the hardware instructions (stop, twld, twst, and, and clr) concatenated
	 * with the register specification for the opcode.
	 */
	private void buildOpCodes() {
		opcodes.put("stop",  "0000");
		opcodes.put("twldj",  "0500");
		opcodes.put("twldk",  "0510");
		opcodes.put("twstj",  "0540");
		opcodes.put("twstk",  "0550");
		opcodes.put("andj",  "1100");
		opcodes.put("andk",  "1200");
		opcodes.put("andjk",  "1300");
		opcodes.put("clrj",  "1510");
		opcodes.put("clrk",  "1610");
		opcodes.put("clrjk",  "1710");
	}
}
