import com.xarql.qoi.HelloWorld;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class HelloWorldTest {

    @Test
    public void testBuildSystem() {
        assertFalse(HelloWorld.getPhrase() == null);
        assertNotEquals(HelloWorld.getPhrase(), "");
    }
}
