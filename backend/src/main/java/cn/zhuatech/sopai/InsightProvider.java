/* 上海如静知华信息科技有限公司 https://www.zhuatech.cn/ */
package cn.zhuatech.sopai;
import org.springframework.stereotype.Component;
import java.util.*;

/**
 * SOP 核验接口；默认逐步骤规则核验，可由企业接入其他分析器。
 *
 * 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。
 */
public interface InsightProvider {
 /**
  * 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。
  */
 record Result(List<String> missing,List<String> criticalMissing,List<String> unknown){
  /**
   * 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。
   */
  public boolean compliant(){return missing.isEmpty()&&unknown.isEmpty();}
 }
 /**
  * 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。
  */
 Result check(String standard,String critical,String completed);
}
/**
 * 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。
 */
@Component class LocalInsightProvider implements InsightProvider {
 /**
  * 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。
  */
 static List<String> items(String text){return Arrays.stream(text.split("[;；]")).map(String::strip).filter(x->!x.isEmpty()).distinct().toList();}
 /**
  * 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。
  */
 public Result check(String standard,String critical,String completed){
  List<String> expected=items(standard),criticalSteps=items(critical),done=items(completed);
  return new Result(expected.stream().filter(x->!done.contains(x)).toList(),criticalSteps.stream().filter(x->!done.contains(x)).toList(),done.stream().filter(x->!expected.contains(x)).toList());
 }
}
