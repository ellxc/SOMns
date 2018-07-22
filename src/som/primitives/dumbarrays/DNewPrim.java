package som.primitives.dumbarrays;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import bd.primitives.Primitive;
import bd.primitives.Specializer;
import som.VM;
import som.interpreter.nodes.ExpressionNode;
import som.interpreter.nodes.nary.BinaryExpressionNode;
import som.vm.constants.Classes;
import som.vmobjects.SClass;
import som.vmobjects.SDumbArray.SImmutableDumbArray;
import som.vmobjects.SDumbArray.SMutableDumbArray;
import som.vmobjects.SDumbArray.STransferDumbArray;
import som.vmobjects.SSymbol;
import tools.dym.Tags.NewArray;


@GenerateNodeFactory
@Primitive(primitive = "darray:new:", selector = "new:", inParser = false,
    specializer = DNewPrim.IsArrayClass.class)
public abstract class DNewPrim extends BinaryExpressionNode {
  public static class IsArrayClass extends Specializer<VM, ExpressionNode, SSymbol> {
    public IsArrayClass(final Primitive prim, final NodeFactory<ExpressionNode> fact,
        final VM vm) {
      super(prim, fact, vm);
    }

    @Override
    public boolean matches(final Object[] args, final ExpressionNode[] argNodes) {
      return args[0] instanceof SClass && ((SClass) args[0]).isArray();
    }
  }

  @Override
  protected boolean isTaggedWithIgnoringEagerness(final Class<?> tag) {
    if (tag == NewArray.class) {
      return true;
    } else {
      return super.isTaggedWithIgnoringEagerness(tag);
    }
  }

  protected static final boolean receiverIsArrayClass(final SClass receiver) {
    return receiver == Classes.arrayClass;
  }

  @Specialization(guards = {"!receiver.isTransferObject()", "!receiver.declaredAsValue()"})
  public static final SMutableDumbArray createArray(final SClass receiver, final long length) {
    return new SMutableDumbArray(length, receiver);
  }

  @Specialization(guards = {"receiver.declaredAsValue()"})
  public static final SImmutableDumbArray createValueArray(final SClass receiver,
      final long length) {
    return new SImmutableDumbArray(length, receiver);
  }

  @Specialization(guards = {"receiver.isTransferObject()"})
  protected static final STransferDumbArray createTransferArray(
      final SClass receiver, final long length) {
    return new STransferDumbArray(length, receiver);
  }
}
