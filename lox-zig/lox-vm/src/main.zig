const std = @import("std");
const chunk = @import("./chunk.zig");

const Vec = @import("./common.zig").Vec;
const Chunk = chunk.Chunk;
const Code = chunk.Code;
const OpCode = chunk.OpCode;

pub fn main() !void {
    var gpa = std.heap.GeneralPurposeAllocator(.{}){};
    const allocator = gpa.allocator();
    defer {
        const leak = gpa.deinit();
        if (leak) {
            @panic("Catch memory leak");
        }
    }

    var chunks = try Chunk.init(allocator);
    defer chunks.deinit();

    try chunks.write(Code{ .opcode = OpCode.@"return" }, 123);
    try chunks.write(Code{ .opcode = OpCode.@"return" }, 123);
    try chunks.write(Code{ .opcode = OpCode.constants }, 50);
    _ = try chunks.addConstant(1.2, 50);

    std.debug.print("{}\n", .{chunks});
}

test "simple test" {
    var list = std.ArrayList(i32).init(std.testing.allocator);
    defer list.deinit(); // try commenting this out and see if zig detects the memory leak!
    try list.append(42);
    try std.testing.expectEqual(@as(i32, 42), list.pop());
}
