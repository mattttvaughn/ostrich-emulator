package mattpvaughn.io.github.emulator;

import mattpvaughn.io.github.emulator.cpu.CPU;

import java.io.File;

// Ostrich Emulator: a gameboy emulator pet project.
// By Matt Vaughn: http://mattpvaughn.github.io/ 

public class EmuMain {

    public static void main(String[] args) {
        // Parse args to get game ROM file name
        File gameRom = new File(args[0]);

        Memory memory = new Memory();
        memory.attachGameFile(gameRom);

        CPU cpu = new CPU.Builder().memory(memory).build();


        Display ppu = new Display(memory);

        while (cpu.hasInstruction()) {
            // Execute instructions
            long cycleCount = cpu.executeInstruction();

            // Update ppu
            ppu.update(cycleCount);
        }
    }
} 
