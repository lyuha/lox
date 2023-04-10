const std = @import("std");
const Allocator = std.mem.Allocator;

pub fn Vec(comptime T: type) type {
    return struct {
        const Self = @This();

        allocator: Allocator,
        items: []T,
        count: usize = 0,

        pub fn init(allocator: Allocator) Allocator.Error!Self {
            return Self.initCapacity(allocator, 0);
        }

        pub fn initCapacity(allocator: Allocator, capacity: usize) Allocator.Error!Self {
            return Self{
                .allocator = allocator,
                .items = try allocator.alloc(T, capacity),
            };
        }

        pub fn deinit(self: *Self) void {
            self.allocator.free(self.items);
        }

        pub fn append(self: *Self, item: T) Allocator.Error!void {
            if (self.count >= self.items.len) {
                const new_size = if (self.items.len < 8) 8 else self.items.len * 2;
                self.items = try self.allocator.realloc(self.items, new_size);
            }
            self.items[self.count] = item;
            self.count += 1;
        }

        pub fn get(self: Self, index: usize) ?T {
            if (self.count == 0 or index >= self.items.len) return null;

            return self.items[index];
        }

        pub fn getLast(self: Self) ?T {
            if (self.count == 0) {
                return null;
            }
            return self.items[self.count - 1];
        }
    };
}
