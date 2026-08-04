package com.huwng.alterna.block;

import com.huwng.alterna.gravity.GravityApi;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Tấm trọng lực đa hướng — thay thế hoàn toàn hướng "GravityCoreBlock" (khối
 * đi vòng 6 mặt). Đặt được nhiều tấm trên các mặt khác nhau của CÙNG 1 vị trí
 * khối (giống cách trồng dây leo/rêu phát sáng lên nhiều mặt), mỗi tấm ứng
 * với 1 hướng trọng lực cố định. Bước vào vùng của tấm nào thì trọng lực đổi
 * sang hướng tấm đó — KHÔNG có khái niệm "đi vòng qua cạnh" nên né hoàn toàn
 * lớp bug góc/cạnh, checkSupportingBlock đặc thù, và cooldown/animation-reset
 * loop đã gặp phải với GravityCoreBlock.
 * <p>
 * Đơn giản hoá so với bản gốc Gravity Changer: bỏ hệ thống "level/range"
 * (tấm hút xa dần), bỏ chế độ "attract/repel" đổi được bằng amethyst, bỏ
 * BlockEntity — mỗi tấm chỉ có 2 trạng thái: có hoặc không, hút thẳng vào
 * đúng hướng đó khi entity chạm tới. Nếu sau này cần "tầm hút xa" hay
 * attract/repel, có thể bổ sung thêm nhưng không phải phần bắt buộc để hệ
 * thống hoạt động.
 */
public class GravityPlatingBlock extends Block {

    public static final MapCodec<GravityPlatingBlock> CODEC = simpleCodec(GravityPlatingBlock::new);

    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;

    private static final double THICKNESS = 1.0 / 16.0;
    private static final VoxelShape DOWN_SHAPE = Block.box(0, 0, 0, 16, THICKNESS * 16, 16);
    private static final VoxelShape UP_SHAPE = Block.box(0, 16 - THICKNESS * 16, 0, 16, 16, 16);
    private static final VoxelShape NORTH_SHAPE = Block.box(0, 0, 0, 16, 16, THICKNESS * 16);
    private static final VoxelShape SOUTH_SHAPE = Block.box(0, 0, 16 - THICKNESS * 16, 16, 16, 16);
    private static final VoxelShape WEST_SHAPE = Block.box(0, 0, 0, THICKNESS * 16, 16, 16);
    private static final VoxelShape EAST_SHAPE = Block.box(16 - THICKNESS * 16, 0, 0, 16, 16, 16);

    // Vùng "hiệu ứng" mở rộng nhẹ ra ngoài khối theo hướng tấm, để chạm vào
    // là đổi hướng ngay chứ không cần lún sâu vào khối mới nhận ra.
    private static final double EFFECT_RANGE = 0.6;

    public GravityPlatingBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.getStateDefinition().any()
                .setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false)
                .setValue(WEST, false).setValue(UP, false).setValue(DOWN, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = Shapes.empty();
        if (state.getValue(UP)) shape = Shapes.or(shape, UP_SHAPE);
        if (state.getValue(DOWN)) shape = Shapes.or(shape, DOWN_SHAPE);
        if (state.getValue(NORTH)) shape = Shapes.or(shape, NORTH_SHAPE);
        if (state.getValue(SOUTH)) shape = Shapes.or(shape, SOUTH_SHAPE);
        if (state.getValue(WEST)) shape = Shapes.or(shape, WEST_SHAPE);
        if (state.getValue(EAST)) shape = Shapes.or(shape, EAST_SHAPE);
        return shape.isEmpty() ? Shapes.block() : shape;
    }

    public static BooleanProperty directionToProperty(Direction direction) {
        return switch (direction) {
            case DOWN -> DOWN;
            case UP -> UP;
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case EAST -> EAST;
        };
    }

    public static boolean hasFace(BlockState state, Direction direction) {
        return state.getValue(directionToProperty(direction));
    }

    private static boolean hasAnyFace(BlockState state) {
        for (Direction direction : Direction.values()) {
            if (hasFace(state, direction)) return true;
        }
        return false;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState existing = context.getLevel().getBlockState(context.getClickedPos());
        Direction plateDirection = context.getClickedFace().getOpposite();
        BlockState base = existing.is(this) ? existing : this.defaultBlockState();
        return base.setValue(directionToProperty(plateDirection), true);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (hasFace(state, direction) && canPlaceOn(level, pos.relative(direction), direction.getOpposite())) {
                return true;
            }
        }
        return false;
    }

    private static boolean canPlaceOn(BlockGetter level, BlockPos pos, Direction side) {
        return level.getBlockState(pos).isFaceSturdy(level, pos, side);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        if (level.isClientSide() || !(entity instanceof Player player)) return;

        Direction currentDir = GravityApi.getDirection(player);

        Direction bestDirection = null;
        double bestDistance = Double.POSITIVE_INFINITY;
        for (Direction plateDirection : Direction.values()) {
            if (!hasFace(state, plateDirection)) continue;

            double distance = distanceToFace(player, pos, plateDirection);

            // NẾU người chơi đang ở trọng lực DOWN và tấm ở trần (UP):
            // Không hút người chơi lên nếu người chơi đang đứng trên sàn bên dưới (onGround) hoặc đầu/chân chưa sát trần.
            if (plateDirection == Direction.UP) {
                if (distance > 0.35 || (currentDir == Direction.DOWN && player.onGround())) {
                    continue;
                }
            }

            if (distance <= EFFECT_RANGE && distance < bestDistance) {
                bestDistance = distance;
                bestDirection = plateDirection;
            }
        }

        if (bestDirection != null && GravityApi.getDirection(player) != bestDirection) {
            GravityApi.setDirection(player, bestDirection);
        }
    }

    /** Khoảng cách (theo trục của plateDirection) từ entity tới mặt phẳng chứa tấm. */
    private static double distanceToFace(Entity entity, BlockPos pos, Direction plateDirection) {
        double faceCoord = switch (plateDirection) {
            case DOWN -> pos.getY() + 1.0; // Tấm ở mặt trên của khối sàn (trọng lực hút xuống) -> mặt phẳng ở Y + 1.0
            case UP -> pos.getY();          // Tấm ở mặt dưới của khối trần (trọng lực hút lên) -> mặt phẳng ở Y
            case NORTH -> pos.getZ() + 1.0;// Tấm ở mặt nam của khối bắc (trọng lực hút bắc) -> mặt phẳng ở Z + 1.0
            case SOUTH -> pos.getZ();       // Tấm ở mặt bắc của khối nam (trọng lực hút nam) -> mặt phẳng ở Z
            case WEST -> pos.getX() + 1.0; // Tấm ở mặt đông của khối tây (trọng lực hút tây) -> mặt phẳng ở X + 1.0
            case EAST -> pos.getX();        // Tấm ở mặt tây của khối đông (trọng lực hút đông) -> mặt phẳng ở X
        };
        double entityCoord = switch (plateDirection.getAxis()) {
            case X -> entity.getX();
            case Y -> entity.getY();
            case Z -> entity.getZ();
        };
        return Math.abs(entityCoord - faceCoord);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!player.isShiftKeyDown()) return InteractionResult.PASS;

        // Sneak + click: gỡ tấm ở đúng mặt vừa click.
        Direction plateDirection = hit.getDirection().getOpposite();
        if (!hasFace(state, plateDirection)) return InteractionResult.PASS;

        BlockState next = state.setValue(directionToProperty(plateDirection), false);
        if (hasAnyFace(next)) {
            level.setBlockAndUpdate(pos, next);
        } else {
            level.removeBlock(pos, false);
        }
        return InteractionResult.SUCCESS;
    }
}
