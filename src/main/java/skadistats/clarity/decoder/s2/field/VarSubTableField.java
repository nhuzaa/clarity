package skadistats.clarity.decoder.s2.field;

import skadistats.clarity.ClarityException;
import skadistats.clarity.decoder.Util;
import skadistats.clarity.decoder.s2.S2UnpackerFactory;
import skadistats.clarity.decoder.unpacker.Unpacker;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.model.state.ArrayEntityState;

import java.util.List;

public class VarSubTableField extends Field {

    private final Unpacker baseUnpacker;

    public VarSubTableField(FieldProperties properties) {
        super(properties);
        baseUnpacker = S2UnpackerFactory.createUnpacker(properties, "uint32");
    }

    @Override
    public void initInitialState(ArrayEntityState state, int idx) {
        state.set(idx, null);
    }

    @Override
    public void accumulateName(FieldPath fp, int pos, List<String> parts) {
        assert fp.last == pos || fp.last >= pos + 2;
        addBasePropertyName(parts);
        if (fp.last > pos) {
            parts.add(Util.arrayIdxToString(fp.path[pos + 1]));
            if (fp.last > pos + 1) {
                properties.getSerializer().accumulateName(fp, pos + 2, parts);
            }
        }
    }

    @Override
    public Unpacker getUnpackerForFieldPath(FieldPath fp, int pos) {
        assert fp.last == pos || fp.last >= pos + 2;
        if (fp.last == pos) {
            return baseUnpacker;
        } else {
            return properties.getSerializer().getUnpackerForFieldPath(fp, pos + 2);
        }
    }

    @Override
    public Field getFieldForFieldPath(FieldPath fp, int pos) {
        assert fp.last == pos || fp.last >= pos + 2;
        if (fp.last == pos) {
            return this;
        } else {
            return properties.getSerializer().getFieldForFieldPath(fp, pos + 2);
        }
    }

    @Override
    public FieldType getTypeForFieldPath(FieldPath fp, int pos) {
        assert fp.last == pos || fp.last >= pos + 2;
        if (fp.last == pos) {
            return properties.getType();
        } else {
            return properties.getSerializer().getTypeForFieldPath(fp, pos + 2);
        }
    }

    @Override
    public Object getValueForFieldPath(FieldPath fp, int pos, ArrayEntityState state) {
        assert fp.last == pos || fp.last >= pos + 2;
        ArrayEntityState subState = state.sub(fp.path[pos]);
        if (fp.last == pos) {
            return subState.length();
        } else if (subState.isSub(fp.path[pos + 1])){
            return properties.getSerializer().getValueForFieldPath(fp, pos + 2, subState.sub(fp.path[pos + 1]));
        } else {
            return null;
        }
    }

    @Override
    public void setValueForFieldPath(FieldPath fp, int pos, ArrayEntityState state, Object value) {
        assert fp.last == pos || fp.last >= pos + 2;
        int i = fp.path[pos];
        ArrayEntityState subState = state.sub(i);
        if (fp.last == pos) {
            subState.capacity((Integer) value, true, properties.getSerializer()::initInitialState);
        } else {
            int j = fp.path[pos + 1];
            subState.capacity(j + 1, false, properties.getSerializer()::initInitialState);
            properties.getSerializer().setValueForFieldPath(fp, pos + 2, subState.sub(j), value);
        }
    }

    @Override
    public FieldPath getFieldPathForName(FieldPath fp, String property) {
        if (property.length() < 5) {
            throw new ClarityException("unresolvable fieldpath");
        }
        String idx = property.substring(0, 4);
        fp.path[fp.last] = Integer.valueOf(idx);
        fp.last++;
        return properties.getSerializer().getFieldPathForName(fp, property.substring(5));
    }

    @Override
    public void collectFieldPaths(FieldPath fp, List<FieldPath> entries, ArrayEntityState state) {
        ArrayEntityState subState = state.sub(fp.path[fp.last]);
        int len = subState.length();
        if (len > 0) {
            fp.last += 2;
            for (int i = 0; i < len; i++) {
                fp.path[fp.last - 1] = i;
                properties.getSerializer().collectFieldPaths(fp, entries, subState.sub(i));
            }
            fp.last -= 2;
        }
    }

}
