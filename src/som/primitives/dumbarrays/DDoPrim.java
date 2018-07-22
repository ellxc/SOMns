package som.primitives.dumbarrays;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ValueProfile;

import bd.primitives.Primitive;
import som.interpreter.nodes.ExpressionNode;
import som.interpreter.nodes.dispatch.BlockDispatchNode;
import som.interpreter.nodes.dispatch.BlockDispatchNodeGen;
import som.interpreter.nodes.nary.BinaryComplexOperation;
import som.interpreter.nodes.specialized.SomLoop;
import som.vmobjects.SBlock;
import som.vmobjects.SDumbArray;


@GenerateNodeFactory
@Primitive(selector = "do:", receiverType = SDumbArray.class, disabled = true)
public abstract class DDoPrim extends BinaryComplexOperation {
  private final ValueProfile storageType = ValueProfile.createClassProfile();

  @Child private BlockDispatchNode block = BlockDispatchNodeGen.create();

  // TODO: tag properly, it is a loop and an access

  private void execBlock(final SBlock block, final Object arg) {
    this.block.executeDispatch(new Object[] {block, arg});
  }

  @Specialization
  public final SDumbArray doObjectArray(final SDumbArray arr, final SBlock block) {
    Object[] storage = arr.getObjectStorage(storageType);
    int length = storage.length;
    try {
      if (SDumbArray.FIRST_IDX < length) {
        execBlock(block, storage[SDumbArray.FIRST_IDX]);
      }
      for (long i = SDumbArray.FIRST_IDX + 1; i < length; i++) {
        execBlock(block, storage[(int) i]);
      }
    } finally {
      if (CompilerDirectives.inInterpreter()) {
        SomLoop.reportLoopCount(length, this);
      }
    }
    return arr;
  }

  @Override
  public boolean isResultUsed(final ExpressionNode child) {
    return false;
  }
}
