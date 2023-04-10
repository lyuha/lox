const std = @import("std");
const Allocator = std.mem.Allocator;
const Vec = @import("./common.zig").Vec;

pub const OpCode = enum {
    @"return",
    constants,

    fn offset(self: OpCode) usize {
        return switch (self) {
            .@"return" => 1,
            .constants => 2,
        };
    }

    pub fn format(self: OpCode, comptime fmt: []const u8, options: std.fmt.FormatOptions, writer: anytype) !void {
        _ = options;
        _ = fmt;

        const fields = @typeInfo(@TypeOf(self)).Enum.fields;

        inline for (fields) |f| {
            if (@enumToInt(self) == f.value) try writer.print("{x:0>4} {s: <10}", .{ f.value, f.name });
        }
    }
};

pub const Code = union(enum) {
    opcode: OpCode,
    operand: usize,

    pub fn format(self: Code, comptime fmt: []const u8, options: std.fmt.FormatOptions, writer: anytype) !void {
        _ = options;
        _ = fmt;

        switch (self) {
            .opcode => |op| try writer.print("{s}", .{op}),
            .operand => |index| try writer.print("{d}", .{index}),
        }
    }
};

pub const Chunk = struct {
    const Self = @This();

    const Value = f32;
    const Line = i16;

    allocator: Allocator,
    code: []Code,
    count: usize = 0,
    values: Vec(Value),
    lines: Vec(Line),

    pub fn init(allocator: Allocator) Allocator.Error!Self {
        return Self.initCapacity(allocator, 0);
    }

    pub fn initCapacity(allocator: Allocator, capacity: usize) Allocator.Error!Self {
        return Self{
            .allocator = allocator,
            .code = try allocator.alloc(Code, capacity),
            .values = try Vec(Value).init(allocator),
            .lines = try Vec(Line).init(allocator),
        };
    }

    pub fn deinit(self: *Self) void {
        self.values.deinit();
        self.lines.deinit();
        self.allocator.free(self.code);
    }

    pub fn write(self: *Self, item: Code, line: Line) Allocator.Error!void {
        if (self.code.len == self.count) {
            const size = if (self.code.len < 8) 8 else self.code.len * 2;
            self.code = try self.allocator.realloc(self.code, size);
        }
        self.code[self.count] = item;
        try self.lines.append(line);
        self.count += 1;
    }

    pub fn addConstant(self: *Self, value: Value, line: Line) Allocator.Error!usize {
        const index = self.values.count;
        try self.values.append(value);
        try self.write(Code{ .operand = index }, line);
        return index;
    }

    pub fn format(self: *const Self, comptime fmt: []const u8, options: std.fmt.FormatOptions, writer: anytype) !void {
        _ = options;
        _ = fmt;
        var iter = self.iterator();

        try writer.print("== Disassemble chunks ==\n", .{});
        while (iter.next()) |inst| {
            const line = inst[0];
            for (inst[1]) |code| {
                switch (code) {
                    .opcode => |op| try writer.print("L{d: <6} {s}", .{ line, op }),
                    .operand => |index| {
                        try writer.print("{d: >8} '{?d:.4}'", .{ index, self.values.get(index) });
                    },
                }
            }
            try writer.print("\n", .{});
        }
        try writer.print("== End of chunks ==\n", .{});
    }

    pub const Iterator = struct {
        chunk: *const Chunk,
        index: usize = 0,

        const Item = struct { Line, []Code };

        pub fn next(it: *Iterator) ?Item {
            if (it.index >= it.chunk.count) return null;

            const start = it.index;
            const offset = switch (it.chunk.code[start]) {
                .opcode => |op| op.offset(),
                else => return null,
            };
            const end = start + offset;

            if (end > it.chunk.count) return null;
            it.index = end;

            return .{ it.chunk.lines.get(start).?, it.chunk.code[start..end] };
        }

        pub fn reset(it: *Iterator) void {
            it.index = 0;
        }
    };

    pub fn iterator(self: *const Self) Iterator {
        return Iterator{
            .chunk = self,
            .index = 0,
        };
    }
};
