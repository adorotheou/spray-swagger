/**
 * Copyright 2013 Getty Imges, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gettyimages.spray.swagger

import scala.reflect.runtime.universe._
import scala.collection.immutable.ListMap
import scala.Option.option2Iterable

object ReflectionUtils {

  def getSymbolAnnotation[K: TypeTag](baseTypeSymbol: Symbol): Option[Annotation] = {
    val annotations = baseTypeSymbol.annotations
    val annotationType = typeOf[K]
    annotations.find(_.tpe =:= annotationType)
  }
  
  def getClassAnnotation[T <: AnyRef: TypeTag, K: TypeTag](): Option[Annotation] = {
    getSymbolAnnotation[K](typeOf[T].typeSymbol.asClass)
  }
  
  def getClassAnnotation[K: TypeTag](objectType: Type): Option[Annotation] = {
    getSymbolAnnotation[K](objectType.typeSymbol.asClass)
  }
  
  def hasClassAnnotation[K: TypeTag](objectType: Type): Boolean = {
    val annotations = objectType.typeSymbol.asClass.annotations
    annotations.find(_.tpe =:= typeOf[K]).isDefined
  }
  
  def getAllMethodAnnotations[K: TypeTag](objectType: Type): Seq[(Annotation, TermSymbol)] = 
    getAllDeclarationAnnotations[K](objectType, _.asTerm.isMethod, _.asTerm)
  
  def getAllFieldAnnotations[K: TypeTag](objectType: Type): Seq[(Annotation, TermSymbol)] = 
    getAllDeclarationAnnotations[K](objectType, _.asTerm.isAccessor, _.asTerm.accessed.asTerm)
  
  private def getAllDeclarationAnnotations[K: TypeTag]
  (objectType: Type, filterFunc: Symbol => Boolean, mapFunc: Symbol => TermSymbol): 
  Seq[(Annotation, TermSymbol)] = {
    objectType.declarations.filter(filterFunc).map(mapFunc).flatMap(symbol => {
      getSymbolAnnotation[K](symbol).map(a => (a, symbol))
    }).toSeq
  }
  
  def getMethodAnnotation[K: TypeTag](objectType: Type, name: String): Option[Annotation] = {
    getSymbolAnnotation[K](objectType.declaration(name: TermName).asTerm)
  }
  
  def getMethodAnnotation[K: TypeTag](objectType: Type, symbol: TermSymbol): Option[Annotation] = {
    getSymbolAnnotation[K](objectType.declaration(symbol.name).asTerm)
  }
  
  def getFieldAnnotation[T: TypeTag, K: TypeTag](name: String): Option[Annotation] = {
    val declarationType = typeOf[T].declaration(name: TermName).asTerm.accessed.asTerm
    getSymbolAnnotation[K](declarationType)
  }
 
  def getFieldAnnotation[K: TypeTag](objectType: Type)(name: String): Option[Annotation] = {
    val declarationType = objectType.declaration(name: TermName).asTerm.accessed.asTerm
    getSymbolAnnotation[K](declarationType)
  }
  
  def getType[T: TypeTag](obj: T) = typeOf[T]
  
  def getType[T: TypeTag](clazz: Class[T]) = typeOf[T]
  
  def getTypeTag[T: TypeTag](obj: T) = typeTag[T]
  
  private def getLiteralJavaAnnotation[T: TypeTag](
    name: String, annotationValues: ListMap[Name, JavaArgument]
  ): Option[T] = annotationValues.get(name: TermName).flatMap(_ match {
    case (LiteralArgument(Constant(x))) => Some(x.asInstanceOf[T])
    case x                              => None
  })
  
  private def getArrayJavaAnnotation(
    name: String, annotationValues: ListMap[Name, JavaArgument]
  ): Option[Array[Annotation]] = annotationValues.get(name: TermName) match {
    case Some(ArrayArgument(arr)) => Some(arr.flatMap( _ match {
      case NestedArgument(ann) => Some(ann)
      case x => None
    }))
    case x => None	
  }
  
  def getStringJavaAnnotation(name: String, annotation: Annotation): Option[String] = {
    getLiteralJavaAnnotation(name, annotation.javaArgs)
  } 
  def getIntJavaAnnotation(name: String, annotation: Annotation): Option[Int] = 
    getLiteralJavaAnnotation(name, annotation.javaArgs)
    
  def getBooleanJavaAnnotation(name: String, annotation: Annotation): Option[Boolean] = 
    getLiteralJavaAnnotation(name, annotation.javaArgs)
    
  def getClassJavaAnnotation[T <: AnyRef: TypeTag](name: String, annotation: Annotation)(implicit mirror: Mirror): Option[Class[T]] = {
    annotation.javaArgs.get(name: TermName).flatMap(_ match {
      case (LiteralArgument(Constant(x: Type))) => Some(mirror.runtimeClass(x.typeSymbol.asClass).asInstanceOf[Class[T]])
      case _ => None
    })
  }
    
  def getArrayJavaAnnotation(name: String, annotation: Annotation): Option[Array[Annotation]] =
    getArrayJavaAnnotation(name, annotation.javaArgs)
    
  def getArrayClassJavaAnnotation(name: String, annotation: Annotation)(implicit mirror: Mirror): Option[Seq[Class[Any]]] = {
    annotation.javaArgs.get(name: TermName) match {
      case Some(ArrayArgument(arr)) => Some(arr flatMap { _ match {
        case LiteralArgument(Constant(x: Type)) => Some(mirror.runtimeClass(x.typeSymbol.asClass).asInstanceOf[Class[Any]])
        case x => None
      }})
      case x => None
    }
  }
    
  def valueSymbols[E <: Enumeration: TypeTag] = {
    val valueType = typeOf[E#Value]
    typeOf[E].members.filter(sym => !sym.isMethod && !sym.isType &&
      sym.typeSignature.baseType(valueType.typeSymbol) =:= valueType
    )
  }
  
  def getOuterType(valueType: Type) = valueType.asInstanceOf[TypeRef].pre
  
  def valueSymbols(valueType: Type) = {
    val enumType = getOuterType(valueType)
    enumType.members.filter(sym => !sym.isMethod && !sym.isType &&
      sym.typeSignature.baseType(valueType.typeSymbol) =:= valueType
    )
  }
  
  def getCompanionObject(`type`: Type)(implicit mirror: Mirror): Any = {
    mirror.reflectModule(`type`.typeSymbol.companionSymbol.asModule).instance 
  }
}