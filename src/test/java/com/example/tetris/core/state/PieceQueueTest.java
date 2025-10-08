package com.example.tetris.core.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.tetris.core.piece.TetrominoType;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("PieceQueue")
class PieceQueueTest {

  private static final class CyclingCountingSupplier implements Supplier<TetrominoType> {

    private final TetrominoType[] vals = TetrominoType.values();
    private int calls = 0;

    @Override
    public TetrominoType get() {
      return vals[(calls++) % vals.length];
    }

    int calls() {
      return calls;
    }
  }

  private static TetrominoType expectedAt(int idx) {
    TetrominoType[] vals = TetrominoType.values();
    return vals[idx % vals.length];
  }

  @Nested
  @DisplayName("constructor")
  class ConstructorTests {

    @Test
    @DisplayName("rejects null supplier")
    void rejects_null_supplier() {
      assertThrows(NullPointerException.class, () -> new PieceQueue(3, null));
    }

    @Test
    @DisplayName("immediately fills to size from supplier")
    void fills_to_size() {
      CyclingCountingSupplier sup = new CyclingCountingSupplier();
      PieceQueue q = new PieceQueue(5, sup);

      List<TetrominoType> preview = q.peek();
      assertEquals(5, preview.size());
      for (int i = 0; i < 5; i++) {
        assertEquals(expectedAt(i), preview.get(i), "mismatch at preview index " + i);
      }
      assertEquals(5, sup.calls(), "supplier called once per initial slot");
    }

    @Test
    @DisplayName("size==0 is allowed but makes pop fail (documented behavior?)")
    void zero_size_queue() {
      CyclingCountingSupplier sup = new CyclingCountingSupplier();
      PieceQueue q = new PieceQueue(0, sup);

      assertTrue(q.peek().isEmpty());
      assertEquals(0, sup.calls());

      assertThrows(java.util.NoSuchElementException.class, q::popAndRefill);
      assertEquals(0, sup.calls(), "no items should be generated for size=0");
    }
  }

  @Nested
  @DisplayName("peek()")
  class PeekTests {

    @Test
    @DisplayName("returns immutable snapshot (not live view)")
    void snapshot_immutable() {
      PieceQueue q = new PieceQueue(3, new CyclingCountingSupplier());
      List<TetrominoType> snap = q.peek();

      assertEquals(3, snap.size());

      TetrominoType popped = q.popAndRefill();
      assertNotNull(popped);
      assertEquals(3, snap.size());
    }

    @Test
    @DisplayName("order is head→tail; index 0 is next to spawn")
    void order_head_to_tail() {
      CyclingCountingSupplier sup = new CyclingCountingSupplier();
      PieceQueue q = new PieceQueue(4, sup);

      List<TetrominoType> p0 = q.peek();
      assertEquals(expectedAt(0), p0.get(0));
      assertEquals(expectedAt(1), p0.get(1));
      assertEquals(expectedAt(2), p0.get(2));
      assertEquals(expectedAt(3), p0.get(3));

      q.popAndRefill();
      List<TetrominoType> p1 = q.peek();
      assertEquals(expectedAt(1), p1.get(0));
      assertEquals(expectedAt(2), p1.get(1));
      assertEquals(expectedAt(3), p1.get(2));
      assertEquals(expectedAt(4), p1.get(3));
    }
  }

  @Nested
  @DisplayName("popAndRefill()")
  class PopAndRefillTests {

    @Test
    @DisplayName("returns head then refills from supplier to keep size constant")
    void pop_then_refill() {
      CyclingCountingSupplier sup = new CyclingCountingSupplier();
      PieceQueue q = new PieceQueue(3, sup);

      TetrominoType first = q.popAndRefill();
      assertEquals(expectedAt(0), first);
      assertEquals(4, sup.calls(), "3 initial + 1 refill");

      List<TetrominoType> after = q.peek();
      assertEquals(expectedAt(1), after.get(0));
      assertEquals(expectedAt(2), after.get(1));
      assertEquals(expectedAt(3), after.get(2));
    }

    @Test
    @DisplayName("multiple pops advance deterministically")
    void multiple_pops_advance() {
      CyclingCountingSupplier sup = new CyclingCountingSupplier();
      PieceQueue q = new PieceQueue(3, sup);

      for (int i = 0; i < 10; i++) {
        TetrominoType t = q.popAndRefill();
        assertEquals(expectedAt(i), t, "pop " + i + " mismatch");
      }
      assertEquals(13, sup.calls());
      assertEquals(3, q.peek().size());
    }

    @Test
    @DisplayName("throws NoSuchElementException if queue is empty (only possible with size==0)")
    void throws_when_empty() {
      PieceQueue q = new PieceQueue(0, new CyclingCountingSupplier());
      assertThrows(java.util.NoSuchElementException.class, q::popAndRefill);
    }
  }

  @Nested
  @DisplayName("integration")
  class IntegrationTests {

    @Test
    @DisplayName("peek/pop/peek keeps rolling window consistent")
    void rolling_window_consistency() {
      CyclingCountingSupplier sup = new CyclingCountingSupplier();
      PieceQueue q = new PieceQueue(5, sup);

      assertIterableEquals(
          List.of(expectedAt(0), expectedAt(1), expectedAt(2), expectedAt(3), expectedAt(4)),
          q.peek()
      );

      assertEquals(expectedAt(0), q.popAndRefill());
      assertIterableEquals(
          List.of(expectedAt(1), expectedAt(2), expectedAt(3), expectedAt(4), expectedAt(5)),
          q.peek()
      );

      assertEquals(expectedAt(1), q.popAndRefill());
      assertEquals(expectedAt(2), q.popAndRefill());
      assertIterableEquals(
          List.of(expectedAt(3), expectedAt(4), expectedAt(5), expectedAt(6), expectedAt(7)),
          q.peek()
      );
    }
  }
}
