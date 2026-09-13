/* 上海如静知华信息科技有限公司 https://www.zhuatech.cn/ */
package cn.zhuatech.sopai;
import org.springframework.stereotype.Component;
import java.util.*;

/** SOP 核验接口；默认逐步骤规则核验，可由企业接入其他分析器。 */
public interface InsightProvider {
 record Result(List<String> missing,List<String> criticalMissing,List<String> unknown){
  public boolean compliant(){return missing.isEmpty()&&unknown.isEmpty();}
 }
 Result check(String standard,String critical,String completed);
}
@Component class LocalInsightProvider implements InsightProvider {
 static List<String> items(String text){return Arrays.stream(text.split("[;；]")).map(String::strip).filter(x->!x.isEmpty()).distinct().toList();}
 public Result check(String standard,String critical,String completed){
  List<String> expected=items(standard),criticalSteps=items(critical),done=items(completed);
  return new Result(expected.stream().filter(x->!done.contains(x)).toList(),criticalSteps.stream().filter(x->!done.contains(x)).toList(),done.stream().filter(x->!expected.contains(x)).toList());
 }
}
