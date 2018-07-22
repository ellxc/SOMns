package som.primitives.dumbarrays;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ValueProfile;

import bd.primitives.Primitive;
import som.interpreter.nodes.nary.UnaryExpressionNode;
import som.vmobjects.SDumbArray;
import som.vmobjects.SDumbArray.SMutableDumbArray;


@GenerateNodeFactory
@Primitive(selector = "copy", receiverType = SDumbArray.class, disabled = true)
public abstract class DCopyPrim extends UnaryExpressionNode {
  private final ValueProfile storageType = ValueProfile.createClassProfile();

  @Specialization
  public final SMutableDumbArray doObjectArray(final SMutableDumbArray receiver) {
    assert !receiver.getSOMClass()
                    .isTransferObject() : "Not yet supported, need to instantiate another class";
    return new SMutableDumbArray(receiver.getStorage(storageType).clone(),
        receiver.getSOMClass());
  }

}
