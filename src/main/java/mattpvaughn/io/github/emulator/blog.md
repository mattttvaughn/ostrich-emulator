# Making a GameBoy editor

A few months ago I came across a great post on Hacker news was titled ["Why did I spend 1.5 months creating a Gameboy emulator?"](https://blog.rekawek.eu/2017/02/09/coffee-gb/) When I first read it, I zero experience with assembly language and the entire post was a distant fantasy to me— something which I would love to do but I wouldn't have a clue where to start. After finishing the wonderful [Nand2Tetris](https://www.nand2tetris.org/) course online and acquiring some background knowledge on how assembly language and memory work, when I stumbled on the same article again I knew I had to make an attempt at my own emulator.

I plan to write this series to document my own process creating a GameBoy emulator. I want it to be a document anyone can use (myself included) to locate resources on GameBoy hardware and emulator creation.

Table of contents:
[Overview](#overview)
[CPU](#cpu)
[Memory](#memory)
[GPU](#gpu)
[Input](#input)
[Sound](#sound)
[Testing](#testing)
[GameBoy Color and GBA](#gameboy color and gba)

### Overview

The blog post that inspired this project for me is a great place to learn about
the architecture of the GameBoy and get a sense for the basic structure of an
emulator

I found [this page](http://www.devrs.com/gb/files/opcodes.html) to be the most
useful in explaining the opcodes AND all of the terminology used to explain
commands (n, nn, r... etc.).

Implementation detail: Handling shorts and bytes as unsigned ints in java: https://stackoverflow.com/questions/397867/port-of-random-generator-from-c-to-java#397997

[Two's complement explanation](https://www.cs.cornell.edu/~tomf/notes/cps104/twoscomp.html)

[Exlanation of push/pop](http://sgate.emt.bme.hu/patai/publications/z80guide/part1.html). Also contains other useful explanations of commands, but I didn't have a need for those.

### CPU

TODO


Explanation of some tricky commands:

[DAA command](https://ehaskins.com/2018-01-30%20Z80%20DAA/)


[Shifts and Rotates](http://jgmalcolm.com/z80/advanced/shif)


I used [this](https://realboyemulator.wordpress.com/2013/01/03/a-look-at-the-game-boy-bootstrap-let-the-fun-begin/) to figure out exactly how the JR command works.


[Implementing half-carry](https://www.reddit.com/r/EmuDev/comments/692n59/gb_questions_about_halfcarry_and_best/?st=jq8m504n&sh=9179583a)

[DECODING Gameboy Z80 OPCODES](https://gb-archive.github.io/salvage/decoding_gbz80_opcodes/Decoding%20Gamboy%20Z80%20Opcodes.html)

### Memory

TODO

[Memory Bank Controller tests](https://github.com/Gekkio/mooneye-gb/tree/master/tests/emulator-only/mbc1)

### GPU

TODO

### Input

TODO

### Sound

TODO

### Testing

TODO

### GameBoy Color and GBA


Sources:
GameBoy CPU manual: http://marc.rawer.de/Gameboy/Docs/GBCPUman.pdf
Why did I spend 1.5 months creating a Gameboy emulator? https://blog.rekawek.eu/2017/02/09/coffee-gb/
The Ultimate Game Boy Talk (33c3): https://www.youtube.com/watch?v=HyzD8pNlpwI
Rednex Game Boy development system: https://github.com/bentley/rgbds
BGB emulator/debugger:  http://bgb.bircd.org/
Everdrive (SD card run GB games): https://krikzz.com/
GameBoy sound hardware: http://gbdev.gg8.se/wiki/articles/Gameboy_sound_hardware
Blargg GameBoy testing framework: http://gbdev.gg8.se/files/roms/blargg-gb-tests/
GameBoy Technical Reference (Pan Docs): http://gbdev.gg8.se/wiki/articles/Pan_Docs
GameBoy Quick Reference: http://gbdev.gg8.se/files/docs/GBCribSheet000129.pdf
GameBoy CPU half-carry explanaton: https://www.reddit.com/r/EmuDev/comments/4ycoix/
GameBoy implemented in Javascript: http://imrannazar.com/GameBoy-Emulation-in-JavaScript%3a-The-CPU
GameBoy opcode summary: http://www.devrs.com/gb/files/opcodes.html
