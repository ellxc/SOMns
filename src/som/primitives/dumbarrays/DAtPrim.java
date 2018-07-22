package som.primitives.dumbarrays;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.api.source.SourceSection;

import bd.primitives.Primitive;
import bd.primitives.Specializer;
import som.VM;
import som.interpreter.Invokable;
import som.interpreter.SArguments;
import som.interpreter.nodes.ExpressionNode;
import som.interpreter.nodes.MessageSendNode;
import som.interpreter.nodes.MessageSendNode.AbstractMessageSendNode;
import som.interpreter.nodes.nary.BinaryBasicOperation;
import som.interpreter.transactions.TxArrayAccessFactory.TxBinaryArrayOpNodeGen;
import som.primitives.dumbarrays.DAtPrim.TxAtPrim;
import som.vm.Symbols;
import som.vm.constants.KernelObj;
import som.vmobjects.SDumbArray;
import som.vmobjects.SSymbol;
import tools.dym.Tags.ArrayRead;


@GenerateNodeFactory
@Primitive(primitive = "darray:at:", selector = "at:", receiverType = SDumbArray.class,
    inParser = false, specializer = TxAtPrim.class)
public abstract class DAtPrim extends BinaryBasicOperation {
  protected static final class TxAtPrim extends Specializer<VM, ExpressionNode, SSymbol> {
    public TxAtPrim(final Primitive prim, final NodeFactory<ExpressionNode> fact,
        final VM vm) {
      super(prim, fact, vm);
    }

    @Override
    public ExpressionNode create(final Object[] arguments,
        final ExpressionNode[] argNodes, final SourceSection section,
        final boolean eagerWrapper) {
      ExpressionNode node = super.create(arguments, argNodes, section, eagerWrapper);

      // TODO: seems a bit expensive,
      // might want to optimize for interpreter first iteration speed
      // TODO: clone in UnitializedDispatchNode.AbstractUninitialized.forAtomic()
      RootNode root = argNodes[0].getRootNode();
      boolean forAtomic;
      if (root instanceof Invokable) {
        forAtomic = ((Invokable) root).isAtomic();
      } else {
        // TODO: need to think about integration with actors, but, that's a
        // later research project
        forAtomic = false;
      }

      if (forAtomic) {
        return TxBinaryArrayOpNodeGen.create((BinaryBasicOperation) node, null, null)
                                     .initialize(section, eagerWrapper);
      } else {
        return node;
      }
    }
  }

  private final ValueProfile storageType = ValueProfile.createClassProfile();

  @Child protected AbstractMessageSendNode exception;

  @Override
  @SuppressWarnings("unchecked")
  public DAtPrim initialize(final SourceSection sourceSection) {
    super.initialize(sourceSection);
    this.exception = MessageSendNode.createGeneric(
        Symbols.symbolFor("signalWith:index:"), null, sourceSection);
    return this;
  }

  @Override
  protected boolean isTaggedWithIgnoringEagerness(final Class<?> tag) {
    if (tag == ArrayRead.class) {
      return true;
    } else {
      return super.isTaggedWithIgnoringEagerness(tag);
    }
  }

  private Object triggerException(final VirtualFrame frame,
      final SDumbArray arr, final long idx) {
    int rcvrIdx = SArguments.RCVR_IDX;
    assert rcvrIdx == 0;
    return exception.doPreEvaluated(frame,
        new Object[] {KernelObj.indexOutOfBoundsClass, arr, idx});
  }

  @Specialization
  public final Object doObjectSArray(final VirtualFrame frame,
      final SDumbArray receiver, final long idx) {
    try {
      return receiver.getStorage(storageType)[(int) idx - 1];
    } catch (IndexOutOfBoundsException e) {
      return triggerException(frame, receiver, idx);
    }
  }

}
