package mattpvaughn.io.github.emulator;

// Display for the ostrich GameBoy emulator
// By Matt Vaughn: http://mattpvaughn.github.io/
//
// Memory map for VRAM (0x8000-0x9FFF):
//      0x8000 to 0x97FF: character data:
//          \-> this is where data for OBJ and BG items will be stored
//          \->
//
// VRAM mapping:
//      \-> 0x8000 to 0x08FFF used for sprites and background tiles
//      \-> 0x8800 to 0x97FF used for
//      \-> 0x9800 to 0x9BFF used for 32x32 tile BG or Window
//      \-> 0x9C00 to 0x9FF used for 32x32 tile BG or Window
//
// OAM Mapping:
//      \-> 0xFE00 to 0xFE9F contains 40 entries for sprite attributes


import mattpvaughn.io.github.emulator.cpu.Util;

public class Display {

    // The memory component of the Gameboy. Addresses 0x8000-0x9FFF represent
    // the video RAM.
    private Memory memory;

    // Screen height and width
    private int screenHeight = 144;
    private int screenWidth = 160;

    // Number of bytes in the BG/Window tileset
    private int BG_WINDOW_TILESET_LENGTH = 4096;

    // Number of bytes in the Window tile map
    private int WINDOW_TILE_MAP_LENGTH = 256;

    // Number of bytes in the BG tile map
    private int BG_TILE_MAP_LENGTH = 256;

    // The LCDC register controls/disables various elements
    // Bit explanations:
    //      bit 7: LCD Power (0 = Off, 1 = On)
    //      bit 6: Window tile map (0 = 9800h-9BFFh, 1 = 9C00h-9FFFh)
    //      bit 5: Window enabled (0 = disabled, 1 = enabled)
    //      bit 4: BG & window tileset (0 = 8800h-97FFh, 1 = 8000h-8FFFh)
    //      bit 3: BG tile map (0 = 9800h-9BFFh, 1 = 9C00h-9FFFh)
    //      bit 2: Sprite size (0 = 8×8, 1 = 8×16)
    //      bit 1: Sprites enabled (0 = disabled, 1 = enabled)
    //      bit 0: BG enabled (0 = disabled, 1 = enabled)
    //          \-> (a disabled background is white!)
    private int LCDC = 0xFF40;

    // Register used to check the status of the LCD and to configure the LCD
    // interrupt.
    // Bit explanations:
    //      bit 6: LY=LYC check enabled. (1 = enabled)
    //      bit 5: Mode 2 OAM check enabled. (1 = enabled)
    //      bit 4: Mode 1 V-blank check enabled. (1 = enabled)
    //      bit 3: Mode 0 H-blank check enabled. (1 = enabled)
    //      bit 2: LY=LYC comparison signal      (1 => LYC=LY), (Read only)
    //      bit 1/0: Screen mode                 (Read only)
    //          0: H-blank
    //          1: V-blank
    //          2: Searching OAM-RAM
    //          3: Transferring data to LCD driver
    // Mode 0: CPU can access VRAM and OAM
    // Mode 1: CPU can access VRAM and OAM
    // Mode 2: CPU can access VRAM (but not OAM)
    // Mode 3: CPU cannot access VRAM or OAM
    private int STAT = 0xFF41;

    // Scroll Y. The number of pixels which the BG map has been scrolled by in
    // the y direction.
    private int SCY = 0xFF42;

    // Scroll X. The number of pixels which the BG map has been scrolled by in
    // the x direction.
    private int SCX = 0xFF43;

    // The current horizontal line being drawn. It can take any value from 0 to
    // 153. Values 144-153 indicate V-blank period.
    private int LY = 0xFF44;

    // Used to trigger an interrupt when LY has the same value as LYC.
    private int LYC = 0xFF45;

    // DMA transfer and start address. Launches a DMA transfer from ROM or RAM
    // to OAM memory (sprite attribute table). The value written to this
    // location specifies the source address, i.e. where values will be taken
    // from and written to VRAM. e.g. writing XX to the value sets the source
    // to be XX00-XX9F.
    private int DMA = 0xFF46;

    // Background palette data. Assigns gray shades to the color numbers of BG
    // and Window tiles
    // Bit explanations:
    //      bit 0-1: Shade for color number 3
    //      bit 2-3: Shade for color number 2
    //      bit 4-5: Shade for color number 1
    //      bit 6-7: Shade for color number 0
    private int BGP = 0xFF47;

    // Object palette 0 data. Assigns gray shades for sprite palette #0.
    // Bit explanations: same as BGP (0xFF47), but 0-1 are always transparent
    private int OBP0 = 0xFF48;

    // Object palette 1 data. Same as OBP0 (0xFF48), but for sprite palette #1
    private int OBP1 = 0xFF49;

    // Window X position minus 7.
    private int WY = 0xFF4A;

    // Window Y position.
    private int WX = 0xFF4B;

    // Colors in the palette, from darkest to lighest
    private int[] colors = new int[]{0x66, 0x99, 0xAA, 0xCC};


    public Display(Memory memory) {
        this.memory = memory;
    }

    // Transfer data from the memory at address XX00-XXFF into OAM (FE00-FE9F)
    // Duration: 160 * 4 + 4 cycles.
    // A transfer will continue to run if the CPU enters HALT or STOP mode, but
    // cannot begin if the CPU is in such a mode.
    public void DMATransfer(byte unsignedAddress) {
        int sourceAddress = Util.unsignedByteToInt(unsignedAddress) << 2;
        memory.loadToOAM(sourceAddress);
    }

    // BG & window tileset starting address (0 = 8800h-97FFh, 1 = 8000h-8FFFh)
    public int getBGWindowTileSetLocation() {
        byte LCDCValue = memory.readByte(LCDC);
        if (Util.checkBit(LCDCValue, (byte) 4)) {
            return 0x8000;
        }
        return 0x8800;
    }

    // Window tile map starting address (0 = 9800h-9BFFh, 1 = 9C00h-9FFFh)
    public int getWindowTileMapLocation() {
        byte LCDCValue = memory.readByte(LCDC);
        if (Util.checkBit(LCDCValue, (byte) 6)) {
            return 0x9C00;
        }
        return 0x9800;
    }

    // BG tile map starting address (0 = 9800h-9BFFh, 1 = 9C00h-9FFFh)
    public int getBGTileMap() {
        byte LCDCValue = memory.readByte(LCDC);
        if (Util.checkBit(LCDCValue, (byte) 3)) {
            return 0x9C00;
        }
        return 0x9800;
    }

    // BG enabled (0 = disabled, 1 = enabled)
    public boolean shouldDrawBG() {
        byte LCDCValue = memory.readByte(LCDC);
        return Util.checkBit(LCDCValue, (byte) 0);
    }

    // Sprite size (0 = 8×8, 1 = 8×16)
    public int spriteHeight() {
        byte LCDCValue = memory.readByte(LCDC);
        if (Util.checkBit(LCDCValue, (byte) 3)) {
            return 16;
        }
        return 8;
    }

    public void drawBackground() {
        // Pull color numbers from BGP. These color numbers combine with the
        byte[] colorNumbers = new byte[4];

        // Pull pixel bytes/bits from memory two at a time. leastSignificantPixelBits
        // contains data for the least significant bits of color number for the
        // current 8 pixels, mostSignificantPixelBits contains the most
        // significant bits;
        int screenX = 0;
        int screenY = 0;

        // X and Y after adjusting by screen offset
        int x = (screenX + memory.readByte(SCX)) % screenWidth;
        int y = (screenY + memory.readByte(SCY)) % screenHeight;

        // Background palette data
        //int color = getBGColor();
    }

    private int getBGColor(byte b) {
        byte palette = memory.readByte(BGP);
        return 0;
    }


    // Sprite attribute table consists of 40 entries of 4 bytes each. Only ten
    // sprites can be drawn per scanline, any more are invisible.
    // Byte explanations:
    //      byte 0: Y position. Vertical position of the sprite. Any offscreen
    //              byte (not 1-159) hides the sprite
    //      byte 1: X position. Horizontal position of the sprite. Any value
    //              offscreen (not 1-167) hides the sprite.
    //      byte 2: Tile number. Selects the sprite from the bank of sprites at
    //              0x8000 to 0x8FFF, as an unsigned byte 0-255. In 8x16 mode,
    //              the lower bit of the tile number is ignored. I.e. the upper
    //              8x8 tile is "NN AND FEh", and the lower 8x8 tile is
    //              "NN OR 01h".
    //      byte 3: Attributes/flags.
    //          bit 7: OBJ/BG priority  (0 = OBJ above BG, 1 = OBJ below BG)
    //          bit 6: Y flip           (0 = normal, 1 = vertically mirrored)
    //          bit 5: X flip           (0 = normal, 1 = horizontally mirrored)
    //          bit 4: Palette number   (0 = OBP0, 1 = OBP1)
    //          bit 3-0: Ignored on DMG (original gameboy)
    //
    // Sprite conflicts: When sprites with different x coordinates overlap, the
    // one with the smallest x coordinate appears on top. When they overlap at
    // the same x coordinate, have priority by table ordering (0xFE00 highest,
    // 0xFE04 second highest...)
    public void drawSprites() {
        // Get the sprite attribute table address

    }


    // Mode 0 is present between 201-207 clks, 2 about 77-83 clks, and 3 about
    // 169-175 clks. A complete cycle through these states takes 456 clks.
    // VBlank lasts 4560 clks. A complete screen refresh occurs every 70224
    // clks.)
    public void update(long cycleCount) {
    }
}
