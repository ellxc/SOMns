package som.primitives.dumbarrays;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.ImportStatic;
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
import som.interpreter.nodes.nary.TernaryExpressionNode;
import som.interpreter.transactions.TxArrayAccessFactory.TxTernaryArrayOpNodeGen;
import som.primitives.dumbarrays.DAtPutPrim.TxAtPutPrim;
import som.vm.Symbols;
import som.vm.constants.KernelObj;
import som.vm.constants.Nil;
import som.vmobjects.SDumbArray;
import som.vmobjects.SDumbArray.SMutableDumbArray;
import som.vmobjects.SSymbol;
import tools.dym.Tags.ArrayWrite;
import tools.dym.Tags.BasicPrimitiveOperation;


@GenerateNodeFactory
@ImportStatic(Nil.class)
@Primitive(primitive = "array:at:put:", selector = "at:put:",
    receiverType = SDumbArray.class, inParser = false, specializer = TxAtPutPrim.class)
public abstract class DAtPutPrim extends TernaryExpressionNode {
  protected static final class TxAtPutPrim extends Specializer<VM, ExpressionNode, SSymbol> {
    public TxAtPutPrim(final Primitive prim, final NodeFactory<ExpressionNode> fact,
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
        // TODO: need to think about integration with actors, but, that's a
        // later research project
      } else {
        forAtomic = false;
      }

      if (forAtomic) {
        return TxTernaryArrayOpNodeGen.create((TernaryExpressionNode) node, null, null, null)
                                      .initialize(section);
      } else {
        return node;
      }
    }
  }

  private final ValueProfile storageType = ValueProfile.createClassProfile();

  @Child protected AbstractMessageSendNode exception;

  @Override
  @SuppressWarnings("unchecked")
  public DAtPutPrim initialize(final SourceSection sourceSection) {
    super.initialize(sourceSection);
    exception = MessageSendNode.createGeneric(
        Symbols.symbolFor("signalWith:index:"), null, sourceSection);
    return this;
  }

  @Override
  protected boolean isTaggedWithIgnoringEagerness(final Class<?> tag) {
    if (tag == BasicPrimitiveOperation.class) {
      return true;
    } else if (tag == ArrayWrite.class) {
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
      final SMutableDumbArray receiver, final long index, final Object value) {
    try {
      receiver.getStorage(storageType)[(int) index - 1] = value;
      return value;
    } catch (IndexOutOfBoundsException e) {
      return triggerException(frame, receiver, index);
    }
  }

}
