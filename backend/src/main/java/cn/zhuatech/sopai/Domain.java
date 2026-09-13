/* 上海如静知华信息科技有限公司 https://www.zhuatech.cn/ */
package cn.zhuatech.sopai;
import org.springframework.stereotype.Component;
import java.util.*;
import java.time.*;
import static cn.zhuatech.sopai.Model.*;
import static cn.zhuatech.sopai.Engine.*;

@Component public class Domain {
 private final InsightProvider insight;
 public Domain(InsightProvider insight){this.insight=insight;}
 static String text(Row r,String key){return txt(r.data(),key);}
 static void steps(Map<String,Object>d){
  var standard=LocalInsightProvider.items(txt(d,"steps"));var critical=LocalInsightProvider.items(txt(d,"criticalSteps"));
  require(!standard.isEmpty()&&!critical.isEmpty()&&critical.stream().allMatch(standard::contains),"关键步骤必须属于标准步骤且至少有一项");
  require(standard.size()<=30,"单份指导书最多 30 个步骤");
 }
 public void create(Engine e,User u,String module,Map<String,Object>d){
  if(module.equals("procedures")){steps(d);require(e.all(u,"procedures").stream().noneMatch(x->text(x,"name").equalsIgnoreCase(txt(d,"name"))&&text(x,"versionName").equalsIgnoreCase(txt(d,"versionName"))),"相同指导书版本已存在");}
  if(module.equals("runs")){Row procedure=e.ref(u,d,"procedure","procedures");require(procedure.state().equals("PUBLISHED"),"只能使用已发布的指导书");require(!date(d,"performedAt").isAfter(LocalDate.now()),"执行日期不能是未来");}
 }
 public void edit(Engine e,User u,Row r,Map<String,Object>d){
  if(r.module().equals("procedures"))steps(d);
  if(r.module().equals("runs")){require(txt(d,"procedure").equals(text(r,"procedure")),"执行记录不能改换指导书版本");require(!date(d,"performedAt").isAfter(LocalDate.now()),"执行日期不能是未来");}
 }
 public String action(Engine e,User u,Row r,String action,Map<String,Object>i,Map<String,Object>d){
  switch(r.module()+"."+action){
   case "procedures.publish" -> {steps(d);d.put("publishedBy",u.username());d.put("publishedAt",Instant.now().toString());}
   case "procedures.retire" -> {require(e.all(u,"runs").stream().noneMatch(x->text(x,"procedure").equals(r.id())&&!x.state().equals("CLOSED")),"仍有未归档执行记录");d.put("retireReason",txt(i,"reason"));}
   case "runs.analyze" -> {
    Row procedure=e.ref(u,d,"procedure","procedures");
    var result=insight.check(text(procedure,"steps"),text(procedure,"criticalSteps"),txt(d,"completedSteps"));
    String outcome=result.compliant()?"COMPLIANT":"DEVIATION";
    e.ledger(u,"checks","RECORDED",Map.of("run",r.id(),"procedure",procedure.id(),"outcome",outcome,"missing",result.missing(),"criticalMissing",result.criticalMissing(),"unknown",result.unknown(),"method","LOCAL_RULES_V1"));
    if(!result.compliant())e.ledger(u,"deviations","OPEN",Map.of("run",r.id(),"severity",result.criticalMissing().isEmpty()?"MEDIUM":"HIGH","missing",result.missing(),"unknown",result.unknown(),"recommendation","补齐标准步骤并提交整改复核"));
    else for(Row deviation:e.all(u,"deviations").stream().filter(x->text(x,"run").equals(r.id())&&x.state().equals("OPEN")).toList()){var resolved=new LinkedHashMap<>(deviation.data());resolved.put("resolvedAt",Instant.now().toString());resolved.put("resolution","重新核验通过");e.save(u,deviation,"RESOLVED",resolved,"RESOLVE","完成整改复核");}
    d.put("lastOutcome",outcome);d.put("missingSteps",result.missing());d.put("criticalMissing",result.criticalMissing());d.put("unknownSteps",result.unknown());d.put("analyzedAt",Instant.now().toString());
    return outcome;
   }
   case "runs.correct" -> {d.put("completedSteps",txt(i,"completedSteps"));d.put("correctiveAction",txt(i,"correctiveAction"));d.put("correctedAt",Instant.now().toString());}
   case "runs.finalize" -> {require(txt(d,"lastOutcome").equals("COMPLIANT"),"仍有未通过的步骤核验");d.put("closedBy",u.username());d.put("closedAt",Instant.now().toString());}
  }
  return null;
 }
 public Map<String,Object> metrics(Engine e,User u){return Map.of("有效指导书",e.all(u,"procedures").stream().filter(x->x.state().equals("PUBLISHED")).count(),"待整改执行",e.all(u,"runs").stream().filter(x->x.state().equals("DEVIATION")).count(),"已归档执行",e.all(u,"runs").stream().filter(x->x.state().equals("CLOSED")).count());}
}
